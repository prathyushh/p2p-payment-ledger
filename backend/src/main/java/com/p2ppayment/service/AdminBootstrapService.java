package com.p2ppayment.service;

import com.p2ppayment.entity.User;
import com.p2ppayment.entity.Wallet;
import com.p2ppayment.repository.UserRepository;
import com.p2ppayment.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class AdminBootstrapService {

    @Value("${app.admin.bootstrap.email:admin@example.com}")
    private String adminEmail;

    @Value("${app.admin.bootstrap.password:}")
    private String adminPassword;

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrapService(UserRepository userRepository, WalletRepository walletRepository,
                                  PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public ApplicationRunner adminBootstrapRunner() {
        return args -> {
            bootstrapAdmin();
        };
    }

    @Transactional
    public void bootstrapAdmin() {
        // Normalize email: trim and lowercase for consistency
        String normalizedAdminEmail = adminEmail.trim().toLowerCase();
        
        Optional<User> existingAdmin = userRepository.findByEmail(normalizedAdminEmail);
        if (existingAdmin.isPresent() && existingAdmin.get().getRole() == User.Role.ADMIN) {
            System.out.println("Admin user already exists: " + normalizedAdminEmail);
            return;
        }

        if (adminPassword == null || adminPassword.isEmpty() || adminPassword.equals("admin@example.com")) {
            System.out.println("ADMIN_BOOTSTRAP_PASSWORD not set. Skipping admin bootstrap.");
            return;
        }

        String hashedPassword = passwordEncoder.encode(adminPassword);
        String hashedPin = passwordEncoder.encode("1234");

        User admin = User.builder()
                .name("Admin User")
                .email(normalizedAdminEmail)
                .phoneNumber("0000000000")
                .password(hashedPassword)
                .transactionPin(hashedPin)
                .role(User.Role.ADMIN)
                .enabled(true)
                .accountLocked(false)
                .build();

        admin = userRepository.save(admin);

        Wallet adminWallet = Wallet.builder()
                .user(admin)
                .balance(BigDecimal.ZERO)
                .dailySpent(BigDecimal.ZERO)
                .status(Wallet.Status.ACTIVE)
                .build();

        walletRepository.save(adminWallet);

        System.out.println("Admin user created successfully: " + normalizedAdminEmail);
    }
}
