package com.p2ppayment.repository;

import com.p2ppayment.entity.SettlementBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SettlementBatchRepository extends JpaRepository<SettlementBatch, Long> {
}