package com.p2ppayment.repository;

import com.p2ppayment.entity.User;
import com.p2ppayment.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUserId(Long userId);
    Optional<Wallet> findByUser(User user);
    
    @Modifying
    @Query("UPDATE Wallet w SET w.dailySpent = 0")
    int resetDailySpent();
}
