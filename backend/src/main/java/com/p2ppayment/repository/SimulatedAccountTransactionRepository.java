package com.p2ppayment.repository;

import com.p2ppayment.entity.SimulatedAccountTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SimulatedAccountTransactionRepository extends JpaRepository<SimulatedAccountTransaction, Long> {
}