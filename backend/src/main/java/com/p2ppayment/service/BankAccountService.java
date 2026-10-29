package com.p2ppayment.service;

import com.p2ppayment.entity.SimulatedAccountTransaction;
import com.p2ppayment.entity.SimulatedBankAccount;
import com.p2ppayment.entity.User;
import com.p2ppayment.repository.SimulatedAccountTransactionRepository;
import com.p2ppayment.repository.SimulatedBankAccountRepository;
import com.p2ppayment.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class BankAccountService {

    private final SimulatedBankAccountRepository bankAccountRepository;
    private final SimulatedAccountTransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public BankAccountService(SimulatedBankAccountRepository bankAccountRepository,
                             SimulatedAccountTransactionRepository transactionRepository,
                             UserRepository userRepository) {
        this.bankAccountRepository = bankAccountRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
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

        return account;
    }

    private String generateUniqueAccountIdentifier() {
        // Generate unique identifier using UUID format: DEMO-XXXXX-XXXXX
        String uuid = UUID.randomUUID().toString().toUpperCase().replace("-", "");
        return "DEMO-" + uuid.substring(0, 10);
    }
}