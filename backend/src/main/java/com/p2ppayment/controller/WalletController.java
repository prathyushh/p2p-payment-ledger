package com.p2ppayment.controller;

import com.p2ppayment.dto.*;
import com.p2ppayment.entity.Wallet;
import com.p2ppayment.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public ResponseEntity<WalletResponse> getWallet() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());

        Wallet wallet = walletService.getWalletByUserId(userId);
        WalletResponse response = toWalletResponse(wallet);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/deactivate")
    public ResponseEntity<WalletResponse> deactivateWallet(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());
        String ipAddress = getClientIpAddress(request);

        Wallet wallet = walletService.deactivateWallet(userId, ipAddress);
        WalletResponse response = toWalletResponse(wallet);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/reactivate")
    public ResponseEntity<WalletResponse> reactivateWallet(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());
        String ipAddress = getClientIpAddress(request);

        Wallet wallet = walletService.reactivateWallet(userId, ipAddress);
        WalletResponse response = toWalletResponse(wallet);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/freeze")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WalletResponse> freezeWallet(@Valid @RequestBody AdminWalletRequest request, HttpServletRequest httpRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long adminId = Long.parseLong(authentication.getName());
        String ipAddress = getClientIpAddress(httpRequest);

        // In a real system, you would fetch the admin user from database
        // For now, we pass null and the service will log the admin ID
        Wallet wallet = walletService.freezeWallet(request.walletId(), null, ipAddress);
        WalletResponse response = toWalletResponse(wallet);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/unfreeze")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WalletResponse> unfreezeWallet(@Valid @RequestBody AdminWalletRequest request, HttpServletRequest httpRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long adminId = Long.parseLong(authentication.getName());
        String ipAddress = getClientIpAddress(httpRequest);

        // In a real system, you would fetch the admin user from database
        Wallet wallet = walletService.unfreezeWallet(request.walletId(), null, ipAddress);
        WalletResponse response = toWalletResponse(wallet);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/top-up")
    public ResponseEntity<WalletTopUpResponse> topUpWallet(@Valid @RequestBody WalletTopUpRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());

        Wallet wallet = walletService.topUp(request.accountId(), userId, request.amount());
        WalletTopUpResponse response = new WalletTopUpResponse(
                null,  // transactionId - would be populated from transaction if needed
                wallet.getId(),
                request.amount(),
                wallet.getBalance(),
                "SUCCESS",
                "Wallet top-up completed successfully"
        );

        return ResponseEntity.ok(response);
    }

    private WalletResponse toWalletResponse(Wallet wallet) {
        BigDecimal dailyRemaining = wallet.getDailyLimit().subtract(wallet.getDailySpent());
        return new WalletResponse(
                wallet.getId(),
                wallet.getBalance(),
                wallet.getDailySpent(),
                wallet.getDailyLimit(),
                dailyRemaining,
                wallet.getStatus().name()
        );
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}