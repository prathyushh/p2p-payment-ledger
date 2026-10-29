package com.p2ppayment.service;

import com.p2ppayment.entity.*;
import com.p2ppayment.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final SimulatedBankAccountRepository simulatedBankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final SimulatedAccountTransactionRepository simulatedAccountTransactionRepository;
    private final AutoReloadService autoReloadService;

    public WalletService(WalletRepository walletRepository, UserRepository userRepository, AuditService auditService,
                         SimulatedBankAccountRepository simulatedBankAccountRepository,
                         TransactionRepository transactionRepository, LedgerEntryRepository ledgerEntryRepository,
                         SimulatedAccountTransactionRepository simulatedAccountTransactionRepository,
                         AutoReloadService autoReloadService) {
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.simulatedBankAccountRepository = simulatedBankAccountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.simulatedAccountTransactionRepository = simulatedAccountTransactionRepository;
        this.autoReloadService = autoReloadService;
    }

    @Transactional(readOnly = true)
    public Wallet getWalletByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return walletRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));
    }

    @Transactional
    public Wallet deactivateWallet(Long userId, String ipAddress) {
        Wallet wallet = getWalletByUserId(userId);

        if (wallet.getStatus() == Wallet.Status.INACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Wallet is already inactive");
        }

        if (wallet.getStatus() == Wallet.Status.FROZEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot deactivate a frozen wallet");
        }

        String beforeStatus = wallet.getStatus().name();
        wallet.setStatus(Wallet.Status.INACTIVE);
        wallet = walletRepository.save(wallet);

        // Log action for user deactivation
        User user = wallet.getUser();
        auditService.logWalletStatusChange("WALLET_DEACTIVATE", wallet.getId(), beforeStatus, Wallet.Status.INACTIVE.name(), user, ipAddress);

        return wallet;
    }

    @Transactional
    public Wallet reactivateWallet(Long userId, String ipAddress) {
        Wallet wallet = getWalletByUserId(userId);

        if (wallet.getStatus() == Wallet.Status.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Wallet is already active");
        }

        if (wallet.getStatus() == Wallet.Status.FROZEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot reactivate a frozen wallet. Contact admin.");
        }

        String beforeStatus = wallet.getStatus().name();
        wallet.setStatus(Wallet.Status.ACTIVE);
        wallet = walletRepository.save(wallet);

        // Log action for user reactivation
        User user = wallet.getUser();
        auditService.logWalletStatusChange("WALLET_REACTIVATE", wallet.getId(), beforeStatus, Wallet.Status.ACTIVE.name(), user, ipAddress);

        return wallet;
    }

    @Transactional
    public Wallet freezeWallet(Long walletId, User adminUser, String ipAddress) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

        if (wallet.getStatus() == Wallet.Status.FROZEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Wallet is already frozen");
        }

        String beforeStatus = wallet.getStatus().name();
        wallet.setStatus(Wallet.Status.FROZEN);
        wallet = walletRepository.save(wallet);

        // Log admin action
        auditService.logWalletStatusChange("WALLET_FREEZE", wallet.getId(), beforeStatus, Wallet.Status.FROZEN.name(), adminUser, ipAddress);

        return wallet;
    }

    @Transactional
    public Wallet unfreezeWallet(Long walletId, User adminUser, String ipAddress) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

        if (wallet.getStatus() != Wallet.Status.FROZEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Wallet is not frozen");
        }

        String beforeStatus = wallet.getStatus().name();
        wallet.setStatus(Wallet.Status.ACTIVE);
        wallet = walletRepository.save(wallet);

        // Log admin action
        auditService.logWalletStatusChange("WALLET_UNFREEZE", wallet.getId(), beforeStatus, Wallet.Status.ACTIVE.name(), adminUser, ipAddress);

        return wallet;
    }

    @Transactional
    public Wallet topUp(Long accountId, Long userId, BigDecimal amount) {
        return topUpInternal(accountId, userId, amount, false);
    }

    @Transactional
    public Wallet topUpInternal(Long accountId, Long userId, BigDecimal amount, boolean skipAutoReloadCheck) {
        // Validate amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be greater than 0");
        }

        // Fetch user and validate
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Fetch wallet and validate
        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

        // Validate wallet is active
        if (wallet.getStatus() != Wallet.Status.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Wallet is not active. Current status: " + wallet.getStatus().name());
        }

        // Fetch simulated bank account and validate ownership
        SimulatedBankAccount account = simulatedBankAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Simulated account not found"));

        if (!account.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this account");
        }

        if (!account.getIsActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Simulated account is inactive");
        }

        // Validate sufficient balance
        if (account.getSimulatedBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient balance in simulated account");
        }

        // Atomic transaction: deduct from account, credit wallet, create records
        // Step 1: Create Transaction record
        String requestId = UUID.randomUUID().toString();
        Transaction transaction = Transaction.builder()
                .requestId(requestId)
                .type(Transaction.Type.WALLET_TOP_UP)
                .receiverWallet(wallet)
                .senderWallet(null)
                .amount(amount)
                .status(Transaction.Status.SUCCESS)
                .simulatedAccount(account)
                .initiatedAt(LocalDateTime.now())
                .settledAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        transaction = transactionRepository.save(transaction);

        // Step 2: Deduct from simulated account
        BigDecimal accountBalanceBefore = account.getSimulatedBalance();
        account.setSimulatedBalance(account.getSimulatedBalance().subtract(amount));
        account.setUpdatedAt(LocalDateTime.now());
        simulatedBankAccountRepository.save(account);

        // Step 3: Create SimulatedAccountTransaction record (DEBIT)
        SimulatedAccountTransaction accountTxn = SimulatedAccountTransaction.builder()
                .simulatedAccount(account)
                .operationType("WALLET_TOP_UP")
                .amount(amount)
                .direction("DEBIT")
                .balanceBefore(accountBalanceBefore)
                .balanceAfter(account.getSimulatedBalance())
                .relatedTransactionId(transaction.getId())
                .outcome("SUCCESS")
                .notes("Top-up to wallet ID " + wallet.getId())
                .createdAt(LocalDateTime.now())
                .build();
        simulatedAccountTransactionRepository.save(accountTxn);

        // Step 4: Credit to wallet
        BigDecimal walletBalanceBefore = wallet.getBalance();
        wallet.setBalance(wallet.getBalance().add(amount));
        wallet = walletRepository.save(wallet);

        // Step 5: Create LedgerEntry (CREDIT)
        LedgerEntry ledgerEntry = LedgerEntry.builder()
                .transaction(transaction)
                .wallet(wallet)
                .type(LedgerEntry.Type.CREDIT)
                .amount(amount)
                .balanceBefore(walletBalanceBefore)
                .balanceAfter(wallet.getBalance())
                .createdAt(LocalDateTime.now())
                .build();
        ledgerEntryRepository.save(ledgerEntry);

        // Log audit trail
        auditService.logWalletStatusChange("WALLET_TOP_UP", wallet.getId(), 
                walletBalanceBefore.toString(), wallet.getBalance().toString(), user, null);

        // Trigger Auto Reload check if not skipping (prevent recursion after AUTO_RELOAD transaction)
        if (!skipAutoReloadCheck) {
            autoReloadService.attemptReload(wallet.getId(), false);
        }

        return wallet;
    }
}