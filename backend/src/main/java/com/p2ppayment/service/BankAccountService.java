package com.p2ppayment.service;

import com.p2ppayment.entity.AutoReloadSettings;
import com.p2ppayment.entity.SimulatedAccountTransaction;
import com.p2ppayment.entity.SimulatedBankAccount;
import com.p2ppayment.entity.User;
import com.p2ppayment.entity.Wallet;
import com.p2ppayment.repository.AutoReloadSettingsRepository;
import com.p2ppayment.repository.SimulatedAccountTransactionRepository;
import com.p2ppayment.repository.SimulatedBankAccountRepository;
import com.p2ppayment.repository.UserRepository;
import com.p2ppayment.repository.WalletRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BankAccountService {

    private final SimulatedBankAccountRepository bankAccountRepository;
    private final SimulatedAccountTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final AutoReloadSettingsRepository autoReloadSettingsRepository;
    private final AutoReloadService autoReloadService;

    public BankAccountService(SimulatedBankAccountRepository bankAccountRepository,
                             SimulatedAccountTransactionRepository transactionRepository,
                             UserRepository userRepository,
                             WalletRepository walletRepository,
                             AutoReloadSettingsRepository autoReloadSettingsRepository,
                             AutoReloadService autoReloadService) {
        this.bankAccountRepository = bankAccountRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.autoReloadSettingsRepository = autoReloadSettingsRepository;
        this.autoReloadService = autoReloadService;
    }

    @Transactional
    public SimulatedBankAccount createBankAccount(Long userId, String bankName, String accountNickname) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Generate unique demo account identifier
        String accountIdentifier = generateUniqueAccountIdentifier();

        SimulatedBankAccount account = SimulatedBankAccount.builder()
                .user(user)
                .bankName(bankName)
                .accountNickname(accountNickname)
                .accountIdentifier(accountIdentifier)
                .simulatedBalance(BigDecimal.ZERO)
                .isActive(true)
                .build();

        return bankAccountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public List<SimulatedBankAccount> listBankAccounts(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return bankAccountRepository.findByUser(user);
    }

    @Transactional(readOnly = true)
    public SimulatedBankAccount getBankAccount(Long accountId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return bankAccountRepository.findByIdAndUser(accountId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found or access denied"));
    }

    @Transactional
    public SimulatedBankAccount deactivateBankAccount(Long accountId, Long userId) {
        SimulatedBankAccount account = getBankAccount(accountId, userId);

        if (!account.getIsActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account is already inactive");
        }

        account.setIsActive(false);
        return bankAccountRepository.save(account);
    }

    @Transactional
    public SimulatedBankAccount addDemoFunds(Long accountId, Long userId, BigDecimal amount) {
        SimulatedBankAccount account = getBankAccount(accountId, userId);

        if (!account.getIsActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot add funds to inactive account");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be greater than zero");
        }

        // Record balance before
        BigDecimal balanceBefore = account.getSimulatedBalance();

        // Add funds
        BigDecimal newBalance = balanceBefore.add(amount);
        account.setSimulatedBalance(newBalance);
        account = bankAccountRepository.save(account);

        // Create audit trail entry
        SimulatedAccountTransaction transaction = SimulatedAccountTransaction.builder()
                .simulatedAccount(account)
                .operationType("DEMO_FUND_ADDED")
                .amount(amount)
                .direction("CREDIT")
                .balanceBefore(balanceBefore)
                .balanceAfter(newBalance)
                .outcome("SUCCESS")
                .notes("Demo funds added")
                .build();

        transactionRepository.save(transaction);

        // Check if this bank account is used for auto-reload and trigger reload if needed
        User user = account.getUser();
        Optional<Wallet> walletOpt = walletRepository.findByUser(user);
        
        if (walletOpt.isPresent()) {
            Wallet wallet = walletOpt.get();
            Optional<AutoReloadSettings> settingsOpt = autoReloadSettingsRepository.findByWallet(wallet);
            
            if (settingsOpt.isPresent()) {
                AutoReloadSettings settings = settingsOpt.get();
                
                // If auto-reload is enabled and uses this bank account, attempt reload
                if (settings.getIsEnabled() && 
                    settings.getSimulatedAccount() != null &&
                    settings.getSimulatedAccount().getId().equals(accountId)) {
                    
                    // Attempt auto-reload after bank funds are added
                    autoReloadService.attemptReload(wallet.getId(), false);
                }
            }
        }

        return account;
    }

    private String generateUniqueAccountIdentifier() {
        // Generate unique identifier using UUID format: DEMO-XXXXX-XXXXX
        String uuid = UUID.randomUUID().toString().toUpperCase().replace("-", "");
        return "DEMO-" + uuid.substring(0, 10);
    }
}