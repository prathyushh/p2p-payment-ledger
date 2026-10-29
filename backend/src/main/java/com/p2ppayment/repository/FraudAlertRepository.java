package com.p2ppayment.repository;

import com.p2ppayment.entity.FraudAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {
    
    @Query("SELECT COUNT(fa) FROM FraudAlert fa WHERE DATE(fa.createdAt) = :alertDate")
    Long countByAlertDateForToday(LocalDate alertDate);

    @Query("SELECT COUNT(fa) FROM FraudAlert fa WHERE fa.status = 'PENDING'")
    Long countPendingAlerts();
}
