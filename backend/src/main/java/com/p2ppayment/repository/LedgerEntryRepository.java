package com.p2ppayment.repository;

import com.p2ppayment.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    /**
     * Reconciliation: sum DEBIT entries only for settled P2P transfers on a given date.
     * Excludes WALLET_TOP_UP and AUTO_RELOAD because those only have CREDIT entries (no matching DEBIT).
     * Only P2P transfers have both DEBIT (sender) and CREDIT (receiver) — those must balance.
     */
    @Query("SELECT COALESCE(SUM(le.amount), 0) FROM LedgerEntry le " +
           "WHERE le.type = 'DEBIT' " +
           "AND DATE(le.createdAt) = :summaryDate " +
           "AND le.transaction.status = 'SUCCESS' " +
           "AND le.transaction.type = 'P2P_TRANSFER'")
    BigDecimal sumDebitForDate(LocalDate summaryDate);

    /**
     * Reconciliation: sum CREDIT entries only for settled P2P transfers on a given date.
     * Excludes WALLET_TOP_UP and AUTO_RELOAD because those only have CREDIT entries (no matching DEBIT).
     * Only P2P transfers have both DEBIT (sender) and CREDIT (receiver) — those must balance.
     */
    @Query("SELECT COALESCE(SUM(le.amount), 0) FROM LedgerEntry le " +
           "WHERE le.type = 'CREDIT' " +
           "AND DATE(le.createdAt) = :summaryDate " +
           "AND le.transaction.status = 'SUCCESS' " +
           "AND le.transaction.type = 'P2P_TRANSFER'")
    BigDecimal sumCreditForDate(LocalDate summaryDate);
}
