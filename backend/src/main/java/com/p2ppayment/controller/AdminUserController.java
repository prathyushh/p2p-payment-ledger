package com.p2ppayment.controller;

import com.p2ppayment.dto.AdminUserResponse;
import com.p2ppayment.dto.TransactionResponse;
import com.p2ppayment.entity.Transaction;
import com.p2ppayment.entity.User;
import com.p2ppayment.entity.Wallet;
import com.p2ppayment.repository.UserRepository;
import com.p2ppayment.repository.WalletRepository;
import com.p2ppayment.repository.TransactionRepository;
import com.p2ppayment.service.AuditService;
import com.p2ppayment.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final WalletService walletService;
    private final AuditService auditService;

    public AdminUserController(UserRepository userRepository,
                              WalletRepository walletRepository,
                              TransactionRepository transactionRepository,
                              WalletService walletService,
                              AuditService auditService) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.walletService = walletService;
        this.auditService = auditService;
    }

    /**
     * GET /api/admin/users
     * List all users with pagination and search.
     */
    @GetMapping
    public ResponseEntity<Page<AdminUserResponse>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<User> users;
        
        if (search != null && !search.isEmpty()) {
            users = userRepository.searchUsers(search, pageable);
        } else {
            users = userRepository.findAllUsers(pageable);
        }
        
        Page<AdminUserResponse> response = users.map(this::mapToAdminUserResponse);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/admin/users/{userId}
     * Get details of a specific user.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<AdminUserResponse> getUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        AdminUserResponse response = mapToAdminUserResponse(user);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/admin/users/{userId}/transactions
     * Get recent transactions for a user.
     */
    @GetMapping("/{userId}/transactions")
    public ResponseEntity<Page<TransactionResponse>> getUserTransactions(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));
        
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<Transaction> transactions = transactionRepository.findBySenderWallet(wallet.getId(), pageable);
        
        Page<TransactionResponse> response = transactions.map(this::mapToTransactionResponse);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/admin/users/{userId}/wallet/freeze
     * Freeze a user's wallet (admin action).
     */
    @PutMapping("/{userId}/wallet/freeze")
    public ResponseEntity<Void> freezeWallet(
            @PathVariable Long userId,
            HttpServletRequest httpRequest) {
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long adminId = Long.parseLong(authentication.getName());
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found"));
        
        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));
        
        String ipAddress = getClientIpAddress(httpRequest);
        walletService.freezeWallet(wallet.getId(), admin, ipAddress);
        
        return ResponseEntity.ok().build();
    }

    /**
     * PUT /api/admin/users/{userId}/wallet/unfreeze
     * Unfreeze a user's wallet (admin action).
     */
    @PutMapping("/{userId}/wallet/unfreeze")
    public ResponseEntity<Void> unfreezeWallet(
            @PathVariable Long userId,
            HttpServletRequest httpRequest) {
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long adminId = Long.parseLong(authentication.getName());
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found"));
        
        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));
        
        String ipAddress = getClientIpAddress(httpRequest);
        walletService.unfreezeWallet(wallet.getId(), admin, ipAddress);
        
        return ResponseEntity.ok().build();
    }

    /**
     * Convert User to AdminUserResponse.
     */
    private AdminUserResponse mapToAdminUserResponse(User user) {
        Wallet wallet = walletRepository.findByUser(user).orElse(null);
        return AdminUserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole().name())
                .walletBalance(wallet != null ? wallet.getBalance() : null)
                .walletStatus(wallet != null ? wallet.getStatus().name() : null)
                .createdAt(user.getCreatedAt())
                .accountLocked(user.getAccountLocked())
                .lockedUntil(user.getLockedUntil())
                .build();
    }

    /**
     * Convert Transaction to TransactionResponse.
     */
    private TransactionResponse mapToTransactionResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getType().name(),
                transaction.getStatus().name(),
                transaction.getAmount(),
                transaction.getSenderWallet().getUser().getEmail(),
                transaction.getReceiverWallet().getUser().getEmail(),
                transaction.getRemark(),
                transaction.getSettlementDueDate().toString(),
                transaction.getInitiatedAt(),
                transaction.getSettledAt()
        );
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

