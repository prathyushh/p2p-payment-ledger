package com.p2ppayment.service;

import com.p2ppayment.entity.FraudAlert;
import com.p2ppayment.entity.LedgerEntry;
import com.p2ppayment.entity.Transaction;
import com.p2ppayment.entity.Wallet;
import com.p2ppayment.repository.FraudAlertRepository;
import com.p2ppayment.repository.LedgerEntryRepository;
import com.p2ppayment.repository.TransactionRepository;
import com.p2ppayment.repository.WalletRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FraudService {

    private final FraudAlertRepository fraudAlertRepository;
    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    // Fraud thresholds
    private static final BigDecimal DAILY_LIMIT = new BigDecimal("50000.00");
    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("8000.00");
    private static final int DUPLICATE_WINDOW_MINUTES = 2;

    public FraudService(FraudAlertRepository fraudAlertRepository,
                        TransactionRepository transactionRepository,
                        WalletRepository walletRepository,
                        LedgerEntryRepository ledgerEntryRepository) {
        this.fraudAlertRepository = fraudAlertRepository;
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    /**
     * Check if sender has exceeded daily send limit.
     * BLOCKS transfer if limit exceeded.
     *
     * @param senderId sender user ID
     * @param currentDailySpent today's already-spent amount
     * @param newAmount amount about to send
     * @return true if check passes (amount does not exceed limit); false if blocked
     */
    @Transactional
    public boolean checkDailyLimit(Long senderId, BigDecimal currentDailySpent, BigDecimal newAmount) {
        BigDecimal totalAfter = currentDailySpent.add(newAmount);
        if (totalAfter.compareTo(DAILY_LIMIT) > 0) {
            // Daily limit exceeded - this will be checked and blocked in TransactionService
            // This method just validates the check
            return false;
        }
        return true;
    }

    /**
     * Check for duplicate transactions within 2-minute window.
     * BLOCKS transfer if duplicate detected (same sender, receiver, amount within 2 minutes).
     * Now uses actual database query to check for recent duplicates.
     *
     * @param senderWalletId sender wallet ID
     * @param receiverWalletId receiver wallet ID
     * @param amount transfer amount
     * @return true if check passes (no duplicate); false if duplicate detected
     */
    @Transactional(readOnly = true)
    public boolean checkDuplicate(Long senderWalletId, Long receiverWalletId, BigDecimal amount) {
        LocalDateTime twoMinutesAgo = LocalDateTime.now().minusMinutes(DUPLICATE_WINDOW_MINUTES);
        
        // Query for recent transactions with same sender, receiver, amount in last 2 minutes
        List<Transaction> recentTransactions = transactionRepository.findRecentDuplicates(
            senderWalletId, receiverWalletId, amount, twoMinutesAgo
        );
        
        return recentTransactions.isEmpty(); // true = no duplicate, false = duplicate found
    }

    /**
     * Check for high-value transfers.
     * ALERTS (does not block) if amount >= ₹8,000.
     * Caller should create alert record separately.
     *
     * @param amount transfer amount
     * @return true if amount is high-value; false otherwise
     */
    public boolean isHighValue(BigDecimal amount) {
        return amount.compareTo(HIGH_VALUE_THRESHOLD) >= 0;
    }

    /**
     * Create a fraud alert record for a transaction.
     * Used for high-value transfers (alert-only rule).
     *
     * @param transaction the transaction to alert on
     * @param alertType type of alert
     * @param notes optional notes
     * @return the created FraudAlert
     */
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
     * Block a fraud alert and refund the associated transaction.
     * Marks alert as BLOCKED, transaction as FAILED, and refunds sender.
     * 
     * @param alertId fraud alert ID
     * @param adminNotes admin notes for blocking
     * @return updated FraudAlert
     */
    @Transactional
    public FraudAlert blockAlert(Long alertId, String adminNotes) {
        FraudAlert alert = fraudAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fraud alert not found"));

        // Check if alert is already processed
        if (alert.getStatus() != FraudAlert.Status.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Alert is already " + alert.getStatus());
        }

        Transaction transaction = alert.getTransaction();
        
        // Check if transaction is already settled or declined - cannot refund
        if (transaction.getStatus() == Transaction.Status.SUCCESS || 
            transaction.getStatus() == Transaction.Status.DECLINED ||
            transaction.getStatus() == Transaction.Status.FAILED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, 
                    "Cannot block: transaction is already " + transaction.getStatus());
        }

        // Refund sender if transaction is PENDING
        if (transaction.getStatus() == Transaction.Status.PENDING) {
            refundTransactionSender(transaction);
        }

        // Mark transaction as FAILED
        transaction.setStatus(Transaction.Status.FAILED);
        transaction.setSettledAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        // Mark alert as BLOCKED
        alert.setStatus(FraudAlert.Status.BLOCKED);
        alert.setNotes(adminNotes != null ? adminNotes : alert.getNotes());
        alert = fraudAlertRepository.save(alert);

        // TODO: TASK-018 will send notifications to sender/receiver about blocking

        return alert;
    }

    /**
     * Dismiss a fraud alert without action on the transaction.
     * Marks alert as DISMISSED; transaction remains unchanged.
     *
     * @param alertId fraud alert ID
     * @param adminNotes admin notes for dismissal
     * @return updated FraudAlert
     */
    @Transactional
    public FraudAlert dismissAlert(Long alertId, String adminNotes) {
        FraudAlert alert = fraudAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fraud alert not found"));

        // Check if alert is already processed
        if (alert.getStatus() != FraudAlert.Status.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Alert is already " + alert.getStatus());
        }

        // Mark alert as DISMISSED
        alert.setStatus(FraudAlert.Status.DISMISSED);
        alert.setNotes(adminNotes != null ? adminNotes : alert.getNotes());
        alert = fraudAlertRepository.save(alert);

        // Transaction remains unchanged (stays PENDING)

        return alert;
    }

    /**
     * Refund a transaction sender: credit wallet and reduce daily_spent.
     * Used internally by blockAlert.
     *
     * @param transaction the transaction to refund
     */
    private void refundTransactionSender(Transaction transaction) {
        Wallet senderWallet = transaction.getSenderWallet();
        if (senderWallet == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sender wallet not found for transaction");
        }

        BigDecimal amount = transaction.getAmount();

        // Credit sender wallet and reduce daily_spent
        BigDecimal balanceBefore = senderWallet.getBalance();
        senderWallet.setBalance(senderWallet.getBalance().add(amount));
        senderWallet.setDailySpent(senderWallet.getDailySpent().subtract(amount));
        senderWallet = walletRepository.save(senderWallet);

        // Create CREDIT ledger entry for refund
        LedgerEntry refundEntry = LedgerEntry.builder()
                .transaction(transaction)
                .wallet(senderWallet)
                .type(LedgerEntry.Type.CREDIT)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(senderWallet.getBalance())
                .createdAt(LocalDateTime.now())
                .build();
        ledgerEntryRepository.save(refundEntry);
    }
}