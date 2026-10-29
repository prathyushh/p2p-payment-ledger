package com.p2ppayment.repository;

import com.p2ppayment.entity.User;
import com.p2ppayment.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUserId(Long userId);
    Optional<Wallet> findByUser(User user);
}