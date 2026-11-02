package com.p2ppayment.repository;

import com.p2ppayment.entity.FraudAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {

    @Query("SELECT COUNT(fa) FROM FraudAlert fa WHERE DATE(fa.createdAt) = :alertDate")
    Long countByAlertDateForToday(LocalDate alertDate);

    @Query("SELECT COUNT(fa) FROM FraudAlert fa WHERE fa.status = 'PENDING'")
    Long countPendingAlerts();

    List<FraudAlert> findAllByOrderByCreatedAtDesc();

    List<FraudAlert> findByStatusOrderByCreatedAtDesc(FraudAlert.Status status);
}
