package com.p2ppayment.repository;

import com.p2ppayment.entity.FraudAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {
}