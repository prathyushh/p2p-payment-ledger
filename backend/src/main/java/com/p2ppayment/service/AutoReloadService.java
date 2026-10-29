package com.p2ppayment.service;

import com.p2ppayment.entity.*;
import com.p2ppayment.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AutoReloadService {

    private final AutoReloadSettingsRepository autoReloadSettingsRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final SimulatedBankAccountRepository simulatedBankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final SimulatedAccountTransactionRepository simulatedAccountTransactionRepository;
    private final JdbcTemplate jdbcTemplate;

    public AutoReloadService(AutoReloadSettingsRepository autoReloadSettingsRepository,
                            WalletRepository walletRepository,
                            UserRepository userRepository,
                            SimulatedBankAccountRepository simulatedBankAccountRepository,
                            TransactionRepository transactionRepository,
                            LedgerEntryRepository ledgerEntryRepository,
                            SimulatedAccountTransactionRepository simulatedAccountTransactionRepository,
                            JdbcTemplate jdbcTemplate) {
        this.autoReloadSettingsRepository = autoReloadSettingsRepository;
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
        this.simulatedBankAccountRepository = simulatedBankAccountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.simulatedAccountTransactionRepository = simulatedAccountTransactionRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public AutoReloadSettings getSettings(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

        return autoReloadSettingsRepository.findByWallet(wallet)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Auto Reload settings not found"));
    }

    @Transactional
    public AutoReloadSettings updateSettings(Long userId, Boolean isEnabled, Long accountId, 
                                            BigDecimal thresholdAmount, BigDecimal reloadAmount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

        // If enabling auto reload, validate all required fields and wallet state
        if (isEnabled != null && isEnabled) {
            // Wallet must be ACTIVE
            if (wallet.getStatus() != Wallet.Status.ACTIVE) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Wallet must be ACTIVE to enable Auto Reload");
            }

            // accountId, thresholdAmount, reloadAmount are required
            if (accountId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "accountId is required when enabling Auto Reload");
            }
            if (thresholdAmount == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "thresholdAmount is required when enabling Auto Reload");
            }
            if (reloadAmount == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reloadAmount is required when enabling Auto Reload");
            }

            // Validate amounts are positive
            if (thresholdAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "thresholdAmount must be greater than 0");
            }
            if (reloadAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reloadAmount must be greater than 0");
            }

            // Validate simulated account exists and belongs to user
            SimulatedBankAccount account = simulatedBankAccountRepository.findById(accountId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Simulated account not found"));

            if (!account.getUser().getId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this account");
            }

            if (!account.getIsActive()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Simulated account must be active to enable Auto Reload");
            }
        }

        // Load or create settings
        AutoReloadSettings settings = autoReloadSettingsRepository.findByWallet(wallet)
                .orElseGet(() -> {
                    AutoReloadSettings newSettings = AutoReloadSettings.builder()
                            .wallet(wallet)
                            .isEnabled(false)
                            .reloadInProgress(false)
                            .build();
                    return autoReloadSettingsRepository.save(newSettings);
                });

        // Update fields
        if (isEnabled != null) {
            settings.setIsEnabled(isEnabled);
        }

        if (isEnabled) {
            // Only set these if enabling
            SimulatedBankAccount account = simulatedBankAccountRepository.findById(accountId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Simulated account not found"));
            settings.setSimulatedAccount(account);
            settings.setThresholdAmount(thresholdAmount);
            settings.setReloadAmount(reloadAmount);
        } else {
            // If disabling, keep existing values (don't clear them)
            // This allows quick re-enable with same settings
            if (accountId != null) {
                SimulatedBankAccount account = simulatedBankAccountRepository.findById(accountId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Simulated account not found"));
                settings.setSimulatedAccount(account);
            }
            if (thresholdAmount != null) {
                settings.setThresholdAmount(thresholdAmount);
            }
            if (reloadAmount != null) {
                settings.setReloadAmount(reloadAmount);
            }
        }

        return autoReloadSettingsRepository.save(settings);
    }

    @Transactional
    public void attemptReload(Long walletId, boolean skipAutoReloadCheck) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElse(null);

        if (wallet == null) {
            return;
        }

        // Skip if caller is requesting skip (prevent recursion after reload credit)
        if (skipAutoReloadCheck) {
            return;
        }

        // Load settings
        AutoReloadSettings settings = autoReloadSettingsRepository.findByWallet(wallet)
                .orElse(null);

        if (settings == null || !settings.getIsEnabled()) {
            return;
        }

        // Check wallet is ACTIVE
        if (wallet.getStatus() != Wallet.Status.ACTIVE) {
            return;
        }

        // Check balance < threshold
        if (wallet.getBalance().compareTo(settings.getThresholdAmount()) >= 0) {
            return;
        }

        // Try to acquire lock using DB-level atomic update
        int rowsAffected = jdbcTemplate.update(
                "UPDATE auto_reload_settings SET reload_in_progress = TRUE WHERE wallet_id = ? AND reload_in_progress = FALSE",
                walletId
        );

        if (rowsAffected != 1) {
            // Another thread/transaction already acquired lock or settings were deleted
            return;
        }

        try {
            // Lock acquired, proceed with reload
            SimulatedBankAccount account = settings.getSimulatedAccount();
            BigDecimal reloadAmount = settings.getReloadAmount();

            // Validate account still exists and is active
            if (account == null || !account.getIsActive()) {
                return;
            }

            // Validate sufficient balance in simulated account
            if (account.getSimulatedBalance().compareTo(reloadAmount) < 0) {
                return;
            }

            // Perform atomic reload: deduct from account, credit wallet, create records
            // Step 1: Create Transaction record
            String requestId = UUID.randomUUID().toString();
            Transaction transaction = Transaction.builder()
                    .requestId(requestId)
                    .type(Transaction.Type.AUTO_RELOAD)
                    .receiverWallet(wallet)
                    .senderWallet(null)
                    .amount(reloadAmount)
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
            account.setSimulatedBalance(account.getSimulatedBalance().subtract(reloadAmount));
            account.setUpdatedAt(LocalDateTime.now());
            simulatedBankAccountRepository.save(account);

            // Step 3: Create SimulatedAccountTransaction record (DEBIT)
            SimulatedAccountTransaction accountTxn = SimulatedAccountTransaction.builder()
                    .simulatedAccount(account)
                    .operationType("AUTO_RELOAD")
                    .amount(reloadAmount)
                    .direction("DEBIT")
                    .balanceBefore(accountBalanceBefore)
                    .balanceAfter(account.getSimulatedBalance())
                    .relatedTransactionId(transaction.getId())
                    .outcome("SUCCESS")
                    .notes("Auto reload for wallet ID " + wallet.getId())
                    .createdAt(LocalDateTime.now())
                    .build();
            simulatedAccountTransactionRepository.save(accountTxn);

            // Step 4: Credit wallet (with flag to skip recursive Auto Reload check)
            BigDecimal walletBalanceBefore = wallet.getBalance();
            wallet.setBalance(wallet.getBalance().add(reloadAmount));
            wallet = walletRepository.save(wallet);

            // Step 5: Create LedgerEntry (CREDIT)
            LedgerEntry ledgerEntry = LedgerEntry.builder()
                    .transaction(transaction)
                    .wallet(wallet)
                    .type(LedgerEntry.Type.CREDIT)
                    .amount(reloadAmount)
                    .balanceBefore(walletBalanceBefore)
                    .balanceAfter(wallet.getBalance())
                    .createdAt(LocalDateTime.now())
                    .build();
            ledgerEntryRepository.save(ledgerEntry);

        } finally {
            // Always release lock, even on failure
            jdbcTemplate.update(
                    "UPDATE auto_reload_settings SET reload_in_progress = FALSE WHERE wallet_id = ?",
                    walletId
            );
        }
    }
}