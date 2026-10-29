package com.p2ppayment.controller;

import com.p2ppayment.dto.AddDemoFundsRequest;
import com.p2ppayment.dto.BankAccountRequest;
import com.p2ppayment.dto.BankAccountResponse;
import com.p2ppayment.entity.SimulatedBankAccount;
import com.p2ppayment.service.BankAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bank-accounts")
public class BankAccountController {

    private final BankAccountService bankAccountService;

    public BankAccountController(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    @PostMapping
    public ResponseEntity<BankAccountResponse> createBankAccount(@Valid @RequestBody BankAccountRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());

        SimulatedBankAccount account = bankAccountService.createBankAccount(userId, request.bankName(), request.accountNickname());
        BankAccountResponse response = toBankAccountResponse(account);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<BankAccountResponse>> listBankAccounts() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());

        List<SimulatedBankAccount> accounts = bankAccountService.listBankAccounts(userId);
        List<BankAccountResponse> responses = accounts.stream()
                .map(this::toBankAccountResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<BankAccountResponse> getBankAccount(@PathVariable Long accountId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());

        SimulatedBankAccount account = bankAccountService.getBankAccount(accountId, userId);
        BankAccountResponse response = toBankAccountResponse(account);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{accountId}/deactivate")
    public ResponseEntity<BankAccountResponse> deactivateBankAccount(@PathVariable Long accountId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());

        SimulatedBankAccount account = bankAccountService.deactivateBankAccount(accountId, userId);
        BankAccountResponse response = toBankAccountResponse(account);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{accountId}/add-demo-funds")
    public ResponseEntity<BankAccountResponse> addDemoFunds(@PathVariable Long accountId, @Valid @RequestBody AddDemoFundsRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());

        SimulatedBankAccount account = bankAccountService.addDemoFunds(accountId, userId, request.amount());
        BankAccountResponse response = toBankAccountResponse(account);

        return ResponseEntity.ok(response);
    }

    private BankAccountResponse toBankAccountResponse(SimulatedBankAccount account) {
        return new BankAccountResponse(
                account.getId(),
                account.getBankName(),
                account.getAccountNickname(),
                account.getAccountIdentifier(),
                account.getSimulatedBalance(),
                account.getIsActive(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}