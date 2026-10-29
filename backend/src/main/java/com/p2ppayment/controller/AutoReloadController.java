package com.p2ppayment.controller;

import com.p2ppayment.dto.AutoReloadRequest;
import com.p2ppayment.dto.AutoReloadResponse;
import com.p2ppayment.entity.AutoReloadSettings;
import com.p2ppayment.service.AutoReloadService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auto-reload")
public class AutoReloadController {

    private final AutoReloadService autoReloadService;

    public AutoReloadController(AutoReloadService autoReloadService) {
        this.autoReloadService = autoReloadService;
    }

    @GetMapping
    public ResponseEntity<AutoReloadResponse> getAutoReloadSettings() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());

        AutoReloadSettings settings = autoReloadService.getSettings(userId);
        AutoReloadResponse response = toAutoReloadResponse(settings);

        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<AutoReloadResponse> updateAutoReloadSettings(
            @Valid @RequestBody AutoReloadRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());

        AutoReloadSettings settings = autoReloadService.updateSettings(
                userId,
                request.isEnabled(),
                request.accountId(),
                request.thresholdAmount(),
                request.reloadAmount()
        );

        AutoReloadResponse response = toAutoReloadResponse(settings);
        return ResponseEntity.ok(response);
    }

    private AutoReloadResponse toAutoReloadResponse(AutoReloadSettings settings) {
        String accountNickname = null;
        if (settings.getSimulatedAccount() != null) {
            accountNickname = settings.getSimulatedAccount().getAccountNickname();
        }

        return new AutoReloadResponse(
                settings.getId(),
                settings.getWallet().getId(),
                settings.getIsEnabled(),
                settings.getSimulatedAccount() != null ? settings.getSimulatedAccount().getId() : null,
                accountNickname,
                settings.getThresholdAmount(),
                settings.getReloadAmount(),
                settings.getCreatedAt(),
                settings.getUpdatedAt()
        );
    }
}