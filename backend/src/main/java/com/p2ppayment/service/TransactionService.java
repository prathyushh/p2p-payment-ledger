package com.p2ppayment.service;

import com.p2ppayment.entity.*;
import com.p2ppayment.repository.*;
import com.p2ppayment.util.BusinessDayCalculator;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final PasswordEncoder passwordEncoder;
    private final AutoReloadService autoReloadService;
    private final FraudService fraudService;
    private final NotificationService notificationService;

    // Daily send limit: Rs 50,000
    private static final BigDecimal DAILY_SEND_LIMIT = new BigDecimal("50000.00");

    public TransactionService(TransactionRepository transactionRepository,
                             WalletRepository walletRepository,
                             UserRepository userRepository,
                             LedgerEntryRepository ledgerEntryRepository,
                             PasswordEncoder passwordEncoder,
                             AutoReloadService autoReloadService,
                             FraudService fraudService,
                             NotificationService notificationService) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.passwordEncoder = passwordEncoder;
        this.autoReloadService = autoReloadService;
        this.fraudService = fraudService;
        this.notificationService = notificationService;
    }

    @Transactional
    public Transaction sendMoney(Long senderId, String recipientIdentifier, BigDecimal amount, 
                                 String remark, String pin) {
        // Validate amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be greater than 0");
        }

        // Fetch sender
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sender not found"));

        Wallet senderWallet = walletRepository.findByUser(sender)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sender wallet not found"));

        // Validate sender wallet is ACTIVE
        if (senderWallet.getStatus() != Wallet.Status.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sender wallet is not active");
        }

        // Validate PIN (BCrypt compare)
        if (pin == null || pin.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PIN is required");
        }

        if (!passwordEncoder.matches(pin, sender.getTransactionPin())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid PIN");
        }

        // Find recipient by email or phone
        Optional<User> recipientOpt = userRepository.findByEmail(recipientIdentifier);
        if (recipientOpt.isEmpty()) {
            recipientOpt = userRepository.findByPhoneNumber(recipientIdentifier);
        }

        User recipient = recipientOpt
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient not found"));

        // Prevent self-transfer
        if (sender.getId().equals(recipient.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot send money to yourself");
        }

        Wallet recipientWallet = walletRepository.findByUser(recipient)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient wallet not found"));

        // Check recipient wallet is not frozen
        if (recipientWallet.getStatus() == Wallet.Status.FROZEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Recipient wallet is frozen");
        }

        // Check sufficient balance
        if (senderWallet.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient balance");
        }

        // ============================================
        // FRAUD CHECKS (before any wallet deduction)
        // ============================================

        // Check 1: Daily limit (BLOCKING)
        BigDecimal todaySent = senderWallet.getDailySpent();
        BigDecimal totalAfterTransfer = todaySent.add(amount);
        if (totalAfterTransfer.compareTo(DAILY_SEND_LIMIT) > 0) {
            // Create fraud alert in separate transaction (won't be rolled back)
            createDailyLimitFraudAlert(sender, recipientWallet, amount, todaySent, totalAfterTransfer, remark);
            
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 
                    "Daily send limit (Rs 50,000) exceeded. Already sent: Rs " + todaySent);
        }

        // Check 2: Duplicate transaction (BLOCKING)
        if (!fraudService.checkDuplicate(senderWallet.getId(), recipientWallet.getId(), amount)) {
            // Create fraud alert in separate transaction (won't be rolled back)
            createDuplicateFraudAlert(sender, recipient, recipientWallet, amount, remark);
            
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 
                    "Duplicate transfer detected. Please wait before retrying.");
        }

        boolean isHighValue = fraudService.isHighValue(amount);

        // ============================================
        // CREATE TRANSACTION (all fraud checks passed)
        // ============================================

        // Generate idempotent request ID (client should provide this, but we generate server-side)
        String requestId = UUID.randomUUID().toString();

        // Calculate settlement due date (3 business days from now)
        LocalDate settlementDueDate = BusinessDayCalculator.addBusinessDays(LocalDate.now(), 3);

        // Create Transaction record (PENDING status)
        final Transaction transaction = transactionRepository.save(Transaction.builder()
                .requestId(requestId)
                .type(Transaction.Type.P2P_TRANSFER)
                .senderWallet(senderWallet)
                .receiverWallet(recipientWallet)
                .amount(amount)
                .status(Transaction.Status.PENDING)
                .remark(remark)
                .settlementDueDate(settlementDueDate)
                .initiatedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        // Create high-value fraud alert if applicable (ALERT ONLY)
        if (isHighValue) {
            fraudService.createAlert(transaction, FraudAlert.AlertType.HIGH_VALUE,
                    "High-value transfer of Rs " + amount + " to " + recipient.getEmail());
            // Notify all admins about high-value transfer
            try {
                userRepository.findAllAdminUsers().forEach(admin -> {
                    try {
                        notificationService.create(
                            admin.getId(),
                            Notification.Type.FRAUD_ALERT_HIGH_VALUE,
                            "High-Value Transfer Alert",
                            "User " + sender.getEmail() + " initiated a high-value transfer of Rs " +
                            amount + " to " + recipient.getEmail() + ". Transaction ID: " + transaction.getId() +
                            ". Please review in the Fraud panel.",
                            transaction
                        );
                    } catch (Exception ex) { /* ignore notification failure */ }
                });
            } catch (Exception ex) { /* ignore notification failure */ }
        }

        // ============================================
        // DEDUCT FROM SENDER WALLET
        // ============================================

        // Deduct from sender wallet and increment daily_spent (atomic operation)
        BigDecimal senderBalanceBefore = senderWallet.getBalance();
        senderWallet.setBalance(senderWallet.getBalance().subtract(amount));
        senderWallet.setDailySpent(senderWallet.getDailySpent().add(amount));
        Wallet updatedSenderWallet = walletRepository.save(senderWallet);

        // Create DEBIT ledger entry for sender
        LedgerEntry debitEntry = LedgerEntry.builder()
                .transaction(transaction)
                .wallet(updatedSenderWallet)
                .type(LedgerEntry.Type.DEBIT)
                .amount(amount)
                .balanceBefore(senderBalanceBefore)
                .balanceAfter(updatedSenderWallet.getBalance())
                .createdAt(LocalDateTime.now())
                .build();
        ledgerEntryRepository.save(debitEntry);

        // Trigger Auto Reload for sender (balance decreased)
        autoReloadService.attemptReload(updatedSenderWallet.getId(), false);

        return transaction;
    }

    /**
     * Get user transaction history with pagination.
     * Includes both sent and received transactions.
     *
     * @param userId user ID
     * @param page page number (0-indexed)
     * @param size page size
     * @return paginated transaction list
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Transaction> getTransactionHistory(Long userId, int page, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

        // Return all transactions (both sent and received)
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return transactionRepository.findByWallet(wallet.getId(), pageable);
    }

    /**
     * Get mini-statement: last 10 transactions (sent + received).
     *
     * @param userId user ID
     * @return list of last 10 transactions
     */
    @Transactional(readOnly = true)
    public java.util.List<Transaction> getMiniStatement(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        return transactionRepository.findMiniStatement(wallet.getId(), pageable);
    }

    /**
     * Get transaction detail with ledger entries.
     *
     * @param transactionId transaction ID
     * @param userId user ID (for authorization check)
     * @return transaction detail with ledger entries
     */
    @Transactional(readOnly = true)
    public Transaction getTransactionDetail(Long transactionId, Long userId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Wallet userWallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

        // Authorization check: user must be sender or receiver
        if (!transaction.getSenderWallet().getId().equals(userWallet.getId()) &&
            !transaction.getReceiverWallet().getId().equals(userWallet.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized to view this transaction");
        }

        return transaction;
    }

    /**
     * Get transaction statistics for user.
     * Includes total sent/received and this month's stats.
     *
     * @param userId user ID
     * @return transaction statistics
     */
    @Transactional(readOnly = true)
    public TransactionStats getTransactionStats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

        Long totalSent = transactionRepository.countSentTransactions(wallet.getId());
        Long totalReceived = transactionRepository.countReceivedTransactions(wallet.getId());
        BigDecimal totalSentAmount = transactionRepository.sumSentAmount(wallet.getId());
        BigDecimal totalReceivedAmount = transactionRepository.sumReceivedAmount(wallet.getId());
        Long thisMonthSent = transactionRepository.countSentThisMonth(wallet.getId());
        Long thisMonthReceived = transactionRepository.countReceivedThisMonth(wallet.getId());
        BigDecimal thisMonthSentAmount = transactionRepository.sumSentThisMonth(wallet.getId());
        BigDecimal thisMonthReceivedAmount = transactionRepository.sumReceivedThisMonth(wallet.getId());

        return new TransactionStats(
            totalSent != null ? totalSent : 0L,
            totalReceived != null ? totalReceived : 0L,
            totalSentAmount != null ? totalSentAmount : BigDecimal.ZERO,
            totalReceivedAmount != null ? totalReceivedAmount : BigDecimal.ZERO,
            thisMonthSent != null ? thisMonthSent : 0L,
            thisMonthReceived != null ? thisMonthReceived : 0L,
            thisMonthSentAmount != null ? thisMonthSentAmount : BigDecimal.ZERO,
            thisMonthReceivedAmount != null ? thisMonthReceivedAmount : BigDecimal.ZERO
        );
    }

    /**
     * Check for duplicate transactions within 2-minute window.
     * Helper method for FraudService (used in sendMoney).
     *
     * @param senderWalletId sender wallet ID
     * @param receiverWalletId receiver wallet ID
     * @param amount transfer amount
     * @return true if no duplicate; false if duplicate found
     */
    @Transactional(readOnly = true)
    public boolean checkDuplicateTransaction(Long senderWalletId, Long receiverWalletId, BigDecimal amount) {
        LocalDateTime twoMinutesAgo = LocalDateTime.now().minusMinutes(2);
        java.util.List<Transaction> recentTransactions = transactionRepository.findRecentDuplicates(
            senderWalletId, receiverWalletId, amount, twoMinutesAgo
        );
        return recentTransactions.isEmpty(); // true = no duplicate
    }

    // Helper class for transaction statistics
    public static class TransactionStats {
        public Long totalSent;
        public Long totalReceived;
        public BigDecimal totalSentAmount;
        public BigDecimal totalReceivedAmount;
        public Long thisMonthSent;
        public Long thisMonthReceived;
        public BigDecimal thisMonthSentAmount;
        public BigDecimal thisMonthReceivedAmount;

        public TransactionStats(Long totalSent, Long totalReceived, BigDecimal totalSentAmount,
                               BigDecimal totalReceivedAmount, Long thisMonthSent, Long thisMonthReceived,
                               BigDecimal thisMonthSentAmount, BigDecimal thisMonthReceivedAmount) {
            this.totalSent = totalSent;
            this.totalReceived = totalReceived;
            this.totalSentAmount = totalSentAmount;
            this.totalReceivedAmount = totalReceivedAmount;
            this.thisMonthSent = thisMonthSent;
            this.thisMonthReceived = thisMonthReceived;
            this.thisMonthSentAmount = thisMonthSentAmount;
            this.thisMonthReceivedAmount = thisMonthReceivedAmount;
        }
    }

    /**
     * Create daily limit fraud alert in a SEPARATE transaction.
     * This ensures the alert is committed even if the main transaction is rolled back.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void createDailyLimitFraudAlert(User sender, Wallet receiverWallet, BigDecimal amount, 
                                          BigDecimal todaySent, BigDecimal totalAfterTransfer, String remark) {
        try {
            // Create FAILED transaction
            Transaction tempTransaction = transactionRepository.save(Transaction.builder()
                    .requestId(UUID.randomUUID().toString())
                    .type(Transaction.Type.P2P_TRANSFER)
                    .senderWallet(walletRepository.findByUser(sender).orElseThrow())
                    .receiverWallet(receiverWallet)
                    .amount(amount)
                    .status(Transaction.Status.FAILED)
                    .remark(remark != null ? remark : "Daily limit exceeded")
                    .settlementDueDate(BusinessDayCalculator.addBusinessDays(LocalDate.now(), 3))
                    .initiatedAt(LocalDateTime.now())
                    .settledAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build());
            
            // Create fraud alert
            fraudService.createAlert(tempTransaction, FraudAlert.AlertType.DAILY_LIMIT_EXCEEDED,
                    "Daily limit exceeded: attempted Rs " + amount + " transfer. Already spent today: Rs " + todaySent);
            
            // Notify all admins
            userRepository.findAllAdminUsers().forEach(admin -> {
                try {
                    notificationService.create(
                        admin.getId(),
                        Notification.Type.FRAUD_ALERT_HIGH_VALUE,
                        "Daily Limit Exceeded Alert",
                        "User " + sender.getEmail() + " attempted to exceed daily limit. " +
                        "Attempted: Rs " + amount + ", Already spent: Rs " + todaySent + 
                        ", Total would be: Rs " + totalAfterTransfer + " (Limit: Rs 50,000). " +
                        "Transaction ID: " + tempTransaction.getId() + ". Please review in the Fraud panel.",
                        tempTransaction
                    );
                } catch (Exception ex) {
                    // Log but don't fail
                }
            });
            
            // Notify sender
            try {
                notificationService.create(
                    sender.getId(),
                    Notification.Type.DAILY_LIMIT_EXCEEDED,
                    "Daily Limit Exceeded",
                    "Your transfer of Rs " + amount + " was blocked because it would exceed your daily limit of Rs 50,000. " +
                    "You have already sent Rs " + todaySent + " today. Please try again tomorrow.",
                    tempTransaction
                );
            } catch (Exception ex) {
                // Log but don't fail
            }
        } catch (Exception e) {
            // Log the error but don't throw - we still want to block the transaction
            System.err.println("Failed to create daily limit fraud alert: " + e.getMessage());
        }
    }

    /**
     * Create duplicate transaction fraud alert in a SEPARATE transaction.
     * This ensures the alert is committed even if the main transaction is rolled back.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void createDuplicateFraudAlert(User sender, User recipient, Wallet recipientWallet, 
                                         BigDecimal amount, String remark) {
        try {
            // Create FAILED transaction
            Transaction tempTransaction = transactionRepository.save(Transaction.builder()
                    .requestId(UUID.randomUUID().toString())
                    .type(Transaction.Type.P2P_TRANSFER)
                    .senderWallet(walletRepository.findByUser(sender).orElseThrow())
                    .receiverWallet(recipientWallet)
                    .amount(amount)
                    .status(Transaction.Status.FAILED)
                    .remark(remark != null ? remark : "Duplicate transaction detected")
                    .settlementDueDate(BusinessDayCalculator.addBusinessDays(LocalDate.now(), 3))
                    .initiatedAt(LocalDateTime.now())
                    .settledAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build());
            
            // Create fraud alert
            fraudService.createAlert(tempTransaction, FraudAlert.AlertType.DUPLICATE_TRANSACTION,
                    "Duplicate transaction detected: Rs " + amount + " to " + recipient.getEmail() + " within 2 minutes");
            
            // Notify all admins
            userRepository.findAllAdminUsers().forEach(admin -> {
                try {
                    notificationService.create(
                        admin.getId(),
                        Notification.Type.FRAUD_ALERT_HIGH_VALUE,
                        "Duplicate Transaction Alert",
                        "User " + sender.getEmail() + " attempted a duplicate transfer of Rs " + amount + 
                        " to " + recipient.getEmail() + " within 2 minutes. " +
                        "Transaction ID: " + tempTransaction.getId() + ". Please review in the Fraud panel.",
                        tempTransaction
                    );
                } catch (Exception ex) {
                    // Log but don't fail
                }
            });
            
            // Notify sender
            try {
                notificationService.create(
                    sender.getId(),
                    Notification.Type.TRANSFER_FAILED,
                    "Duplicate Transaction Detected",
                    "Your transfer of Rs " + amount + " to " + recipient.getEmail() + 
                    " was blocked because a similar transaction was detected within the last 2 minutes. " +
                    "Please wait before retrying.",
                    tempTransaction
                );
            } catch (Exception ex) {
                // Log but don't fail
            }
        } catch (Exception e) {
            // Log the error but don't throw - we still want to block the transaction
            System.err.println("Failed to create duplicate transaction fraud alert: " + e.getMessage());
        }
    }
}

