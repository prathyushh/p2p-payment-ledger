package com.p2ppayment.repository;

import com.p2ppayment.entity.AutoReloadSettings;
import com.p2ppayment.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AutoReloadSettingsRepository extends JpaRepository<AutoReloadSettings, Long> {
    Optional<AutoReloadSettings> findByWallet(Wallet wallet);
}