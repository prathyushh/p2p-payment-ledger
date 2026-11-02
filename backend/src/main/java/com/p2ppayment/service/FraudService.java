package com.p2ppayment.service;

import com.p2ppayment.entity.FraudAlert;
import com.p2ppayment.entity.LedgerEntry;
import com.p2ppayment.entity.Notification;
import com.p2ppayment.entity.Transaction;
import com.p2ppayment.entity.Wallet;
import com.p2ppayment.repository.FraudAlertRepository;
import com.p2ppayment.repository.LedgerEntryRepository;
import com.p2ppayment.repository.TransactionRepository;
import com.p2ppayment.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FraudService {

    private static final Logger logger = LoggerFactory.getLogger(FraudService.class);

    private final FraudAlertRepository fraudAlertRepository;
    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final NotificationService notificationService;

    private static final BigDecimal DAILY_LIMIT = new BigDecimal("50000.00");
    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("8000.00");
    private static final int DUPLICATE_WINDOW_MINUTES = 2;

    public FraudService(FraudAlertRepository fraudAlertRepository,
                        TransactionRepository transactionRepository,
                        WalletRepository walletRepository,
                        LedgerEntryRepository ledgerEntryRepository,
                        NotificationService notificationService) {
        this.fraudAlertRepository = fraudAlertRepository;
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public boolean checkDailyLimit(Long senderId, BigDecimal currentDailySpent, BigDecimal newAmount) {
        BigDecimal totalAfter = currentDailySpent.add(newAmount);
        return totalAfter.compareTo(DAILY_LIMIT) <= 0;
    }

    @Transactional(readOnly = true)
    public boolean checkDuplicate(Long senderWalletId, Long receiverWalletId, BigDecimal amount) {
        LocalDateTime twoMinutesAgo = LocalDateTime.now().minusMinutes(DUPLICATE_WINDOW_MINUTES);
        List<Transaction> recentTransactions = transactionRepository.findRecentDuplicates(
                senderWalletId, receiverWalletId, amount, twoMinutesAgo);
        return recentTransactions.isEmpty();
    }

    public boolean isHighValue(BigDecimal amount) {
        return amount.compareTo(HIGH_VALUE_THRESHOLD) >= 0;
    }

    @Transactional
    public FraudAlert createAlert(Transaction transaction, FraudAlert.AlertType alertType, String notes) {
        FraudAlert alert = FraudAlert.builder()
                .transaction(transaction)
                .alertType(alertType)
                .status(FraudAlert.Status.PENDING)
                .notes(notes)
                .createdAt(LocalDateTime.now())
                .build();
        return fraudAlertRepository.save(alert);
    }

    /**
     * Block a fraud alert:
     * 1. Refunds sender wallet
     * 2. Creates CREDIT ledger entry
     * 3. Marks transaction FAILED
     * 4. Marks alert BLOCKED
     * 5. Sends TRANSFER_FAILED notification to sender
     */
    @Transactional
    public FraudAlert blockAlert(Long alertId, String adminNotes) {
        FraudAlert alert = fraudAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fraud alert not found"));

        if (alert.getStatus() != FraudAlert.Status.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Alert is already " + alert.getStatus());
        }

        Transaction transaction = alert.getTransaction();

        if (transaction.getStatus() != Transaction.Status.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot block: transaction is already " + transaction.getStatus());
        }

        // 1. Refund sender
        Wallet senderWallet = refundTransactionSender(transaction);

        // 2. Mark transaction FAILED
        transaction.setStatus(Transaction.Status.FAILED);
        transaction.setSettledAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        // 3. Mark alert BLOCKED
        alert.setStatus(FraudAlert.Status.BLOCKED);
        alert.setNotes(adminNotes != null ? adminNotes : alert.getNotes());
        alert = fraudAlertRepository.save(alert);

        logger.info("[FRAUD] Alert {} BLOCKED. Transaction {} marked FAILED. Sender refunded Rs{}",
                alertId, transaction.getId(), transaction.getAmount());

        // 4. Notify sender
        try {
            notificationService.create(
                    senderWallet.getUser().getId(),
                    Notification.Type.TRANSFER_FAILED,
                    "Transfer Blocked by Admin",
                    "Your transfer of Rs " + transaction.getAmount() +
                    " has been blocked by admin due to a fraud alert. " +
                    "Rs " + transaction.getAmount() + " has been refunded to your wallet.",
                    transaction
            );
            logger.info("[FRAUD] Refund notification sent to sender: {}", senderWallet.getUser().getEmail());
        } catch (Exception e) {
            logger.error("[FRAUD] Failed to send notification to sender", e);
        }

        // 5. Notify receiver (if applicable)
        try {
            Wallet receiverWallet = transaction.getReceiverWallet();
            if (receiverWallet != null) {
                notificationService.create(
                        receiverWallet.getUser().getId(),
                        Notification.Type.TRANSFER_FAILED,
                        "Incoming Transfer Blocked",
                        "An incoming transfer of Rs " + transaction.getAmount() +
                        " to your account has been blocked by admin due to a fraud alert.",
                        transaction
                );
                logger.info("[FRAUD] Block notification sent to receiver: {}", receiverWallet.getUser().getEmail());
            }
        } catch (Exception e) {
            logger.error("[FRAUD] Failed to send notification to receiver", e);
        }

        return alert;
    }

    /**
     * Dismiss a fraud alert — transaction stays PENDING, no refund.
     */
    @Transactional
    public FraudAlert dismissAlert(Long alertId, String adminNotes) {
        FraudAlert alert = fraudAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fraud alert not found"));

        if (alert.getStatus() != FraudAlert.Status.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Alert is already " + alert.getStatus());
        }

        alert.setStatus(FraudAlert.Status.DISMISSED);
        alert.setNotes(adminNotes != null ? adminNotes : alert.getNotes());
        alert = fraudAlertRepository.save(alert);

        logger.info("[FRAUD] Alert {} DISMISSED. Transaction {} remains PENDING.", alertId, alert.getTransaction().getId());

        return alert;
    }

    /**
     * Refund sender: credit wallet, reduce daily_spent, create CREDIT ledger entry.
     */
    private Wallet refundTransactionSender(Transaction transaction) {
        Wallet senderWallet = transaction.getSenderWallet();
        if (senderWallet == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sender wallet not found");
        }

        BigDecimal amount = transaction.getAmount();
        BigDecimal balanceBefore = senderWallet.getBalance();

        senderWallet.setBalance(senderWallet.getBalance().add(amount));
        // Only subtract daily_spent if it won't go negative
        if (senderWallet.getDailySpent().compareTo(amount) >= 0) {
            senderWallet.setDailySpent(senderWallet.getDailySpent().subtract(amount));
        } else {
            senderWallet.setDailySpent(BigDecimal.ZERO);
        }
        senderWallet = walletRepository.save(senderWallet);

        ledgerEntryRepository.save(LedgerEntry.builder()
                .transaction(transaction)
                .wallet(senderWallet)
                .type(LedgerEntry.Type.CREDIT)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(senderWallet.getBalance())
                .createdAt(LocalDateTime.now())
                .build());

        logger.info("[FRAUD] Refunded Rs{} to sender wallet {}. Balance: {} -> {}",
                amount, senderWallet.getId(), balanceBefore, senderWallet.getBalance());

        return senderWallet;
    }
}
