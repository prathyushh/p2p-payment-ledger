package com.p2ppayment.service;

import com.p2ppayment.entity.*;
import com.p2ppayment.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SettlementService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public SettlementService(TransactionRepository transactionRepository,
                            WalletRepository walletRepository,
                            LedgerEntryRepository ledgerEntryRepository,
                            UserRepository userRepository,
                            NotificationService notificationService) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<Transaction> getPendingTransactionsDue() {
        return transactionRepository.findDueTransactions(LocalDate.now());
    }

    @Transactional
    public void settleTransaction(Transaction transaction) throws Exception {
        try {
            transaction = transactionRepository.findById(transaction.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

            if (transaction.getStatus() != Transaction.Status.PENDING) {
                return;
            }

            Wallet receiverWallet = transaction.getReceiverWallet();
            if (receiverWallet == null) {
                throw new Exception("Receiver wallet not found for transaction " + transaction.getId());
            }

            Wallet senderWallet = transaction.getSenderWallet();
            if (senderWallet == null) {
                throw new Exception("Sender wallet not found for transaction " + transaction.getId());
            }

            BigDecimal amount = transaction.getAmount();

            if (receiverWallet.getStatus() == Wallet.Status.INACTIVE || receiverWallet.getStatus() == Wallet.Status.FROZEN) {
                handleDeclinedSettlement(transaction, senderWallet, amount);
            } else if (receiverWallet.getStatus() == Wallet.Status.ACTIVE) {
                handleSuccessfulSettlement(transaction, senderWallet, receiverWallet, amount);
            }

            transaction.setSettledAt(LocalDateTime.now());
            transactionRepository.save(transaction);

        } catch (Exception e) {
            throw new Exception("Error settling transaction " + transaction.getId() + ": " + e.getMessage(), e);
        }
    }

    private void handleDeclinedSettlement(Transaction transaction, Wallet senderWallet, BigDecimal amount) {
        transaction.setStatus(Transaction.Status.DECLINED);

        BigDecimal senderBalanceBefore = senderWallet.getBalance();
        senderWallet.setBalance(senderWallet.getBalance().add(amount));
        senderWallet.setDailySpent(senderWallet.getDailySpent().subtract(amount));
        senderWallet = walletRepository.save(senderWallet);

        LedgerEntry refundEntry = LedgerEntry.builder()
                .transaction(transaction)
                .wallet(senderWallet)
                .type(LedgerEntry.Type.CREDIT)
                .amount(amount)
                .balanceBefore(senderBalanceBefore)
                .balanceAfter(senderWallet.getBalance())
                .createdAt(LocalDateTime.now())
                .build();
        ledgerEntryRepository.save(refundEntry);
        
        // Create notification for declined settlement
        try {
            notificationService.create(
                senderWallet.getUser().getId(),
                Notification.Type.TRANSFER_FAILED,
                "Transfer Failed",
                "Your transfer of Rs " + amount + " was not settled because the recipient wallet is inactive or frozen. Amount refunded.",
                transaction
            );
        } catch (Exception e) {
            // Log but do not fail settlement if notification fails
        }
    }

    private void handleSuccessfulSettlement(Transaction transaction, Wallet senderWallet, Wallet receiverWallet, BigDecimal amount) {
        transaction.setStatus(Transaction.Status.SUCCESS);

        BigDecimal receiverBalanceBefore = receiverWallet.getBalance();
        receiverWallet.setBalance(receiverWallet.getBalance().add(amount));
        receiverWallet = walletRepository.save(receiverWallet);

        LedgerEntry creditEntry = LedgerEntry.builder()
                .transaction(transaction)
                .wallet(receiverWallet)
                .type(LedgerEntry.Type.CREDIT)
                .amount(amount)
                .balanceBefore(receiverBalanceBefore)
                .balanceAfter(receiverWallet.getBalance())
                .createdAt(LocalDateTime.now())
                .build();
        ledgerEntryRepository.save(creditEntry);
        
        // Create notifications for successful settlement
        try {
            // Notify sender
            notificationService.create(
                senderWallet.getUser().getId(),
                Notification.Type.TRANSFER_SETTLED,
                "Transfer Settled",
                "Your transfer of Rs " + amount + " to " + receiverWallet.getUser().getEmail() + " has been settled.",
                transaction
            );
        } catch (Exception e) {
            // Log but do not fail settlement
        }
        
        try {
            // Notify receiver
            notificationService.create(
                receiverWallet.getUser().getId(),
                Notification.Type.TRANSFER_SETTLED,
                "Transfer Received",
                "You received Rs " + amount + " from " + senderWallet.getUser().getEmail() + ".",
                transaction
            );
        } catch (Exception e) {
            // Log but do not fail settlement
        }
    }
}
