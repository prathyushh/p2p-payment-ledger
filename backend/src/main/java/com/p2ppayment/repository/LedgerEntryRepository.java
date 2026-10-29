package com.p2ppayment.repository;

import com.p2ppayment.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    
    @Query("SELECT COALESCE(SUM(le.amount), 0) FROM LedgerEntry le WHERE le.type = 'DEBIT' AND DATE(le.createdAt) = :summaryDate")
    BigDecimal sumDebitForDate(LocalDate summaryDate);

    @Query("SELECT COALESCE(SUM(le.amount), 0) FROM LedgerEntry le WHERE le.type = 'CREDIT' AND DATE(le.createdAt) = :summaryDate")
    BigDecimal sumCreditForDate(LocalDate summaryDate);
}
