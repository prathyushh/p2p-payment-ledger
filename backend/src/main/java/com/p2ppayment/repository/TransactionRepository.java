package com.p2ppayment.repository;

import com.p2ppayment.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByRequestId(String requestId);

    @Query("SELECT t FROM Transaction t WHERE t.status = 'PENDING' AND t.settlementDueDate <= :today ORDER BY t.settlementDueDate ASC")
    List<Transaction> findDueTransactions(LocalDate today);

    // Combined transaction history (sent + received but excluding PENDING incoming)
    @Query("SELECT t FROM Transaction t WHERE (t.senderWallet.id = :walletId) OR (t.receiverWallet.id = :walletId AND t.status = 'SUCCESS') ORDER BY t.createdAt DESC")
    Page<Transaction> findByWallet(Long walletId, Pageable pageable);

    // Transaction history queries for sender
    @Query("SELECT t FROM Transaction t WHERE t.senderWallet.id = :walletId ORDER BY t.createdAt DESC")
    Page<Transaction> findBySenderWallet(Long walletId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.senderWallet.id = :walletId AND t.type = :type ORDER BY t.createdAt DESC")
    Page<Transaction> findBySenderWalletAndType(Long walletId, Transaction.Type type, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.senderWallet.id = :walletId AND t.status = :status ORDER BY t.createdAt DESC")
    Page<Transaction> findBySenderWalletAndStatus(Long walletId, Transaction.Status status, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.senderWallet.id = :walletId AND t.createdAt >= :startDate AND t.createdAt < :endDate ORDER BY t.createdAt DESC")
    Page<Transaction> findBySenderWalletAndDateRange(Long walletId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.senderWallet.id = :walletId AND t.type = :type AND t.status = :status ORDER BY t.createdAt DESC")
    Page<Transaction> findBySenderWalletTypeAndStatus(Long walletId, Transaction.Type type, Transaction.Status status, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.senderWallet.id = :walletId AND t.type = :type AND t.createdAt >= :startDate AND t.createdAt < :endDate ORDER BY t.createdAt DESC")
    Page<Transaction> findBySenderWalletTypeAndDateRange(Long walletId, Transaction.Type type, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.senderWallet.id = :walletId AND t.status = :status AND t.createdAt >= :startDate AND t.createdAt < :endDate ORDER BY t.createdAt DESC")
    Page<Transaction> findBySenderWalletStatusAndDateRange(Long walletId, Transaction.Status status, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.senderWallet.id = :walletId AND t.type = :type AND t.status = :status AND t.createdAt >= :startDate AND t.createdAt < :endDate ORDER BY t.createdAt DESC")
    Page<Transaction> findBySenderWalletTypeStatusAndDateRange(Long walletId, Transaction.Type type, Transaction.Status status, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    // Transaction history queries for receiver
    // Receivers should ONLY see SETTLED (SUCCESS) incoming money, NOT PENDING
    @Query("SELECT t FROM Transaction t WHERE t.receiverWallet.id = :walletId AND t.status = 'SUCCESS' ORDER BY t.createdAt DESC")
    Page<Transaction> findByReceiverWallet(Long walletId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.receiverWallet.id = :walletId AND t.status = 'SUCCESS' AND t.type = :type ORDER BY t.createdAt DESC")
    Page<Transaction> findByReceiverWalletAndType(Long walletId, Transaction.Type type, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.receiverWallet.id = :walletId AND t.status = :status ORDER BY t.createdAt DESC")
    Page<Transaction> findByReceiverWalletAndStatus(Long walletId, Transaction.Status status, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.receiverWallet.id = :walletId AND t.status = 'SUCCESS' AND t.createdAt >= :startDate AND t.createdAt < :endDate ORDER BY t.createdAt DESC")
    Page<Transaction> findByReceiverWalletAndDateRange(Long walletId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    // Mini-statement: last 10 transactions (sent OR received-and-settled combined)
    // Receivers should NOT see PENDING incoming money
    @Query("SELECT t FROM Transaction t WHERE (t.senderWallet.id = :walletId) OR (t.receiverWallet.id = :walletId AND t.status = 'SUCCESS') ORDER BY t.createdAt DESC")
    List<Transaction> findMiniStatement(Long walletId, Pageable pageable);

    // Duplicate detection query: same sender, receiver, amount within 2-minute window
    @Query("SELECT t FROM Transaction t WHERE t.senderWallet.id = :senderWalletId AND t.receiverWallet.id = :receiverWalletId AND t.amount = :amount AND t.createdAt > :twoMinutesAgo AND t.status IN ('PENDING', 'SUCCESS')")
    List<Transaction> findRecentDuplicates(Long senderWalletId, Long receiverWalletId, BigDecimal amount, LocalDateTime twoMinutesAgo);

    // Stats queries
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.senderWallet.id = :walletId AND t.type = 'P2P_TRANSFER' AND t.status IN ('SUCCESS', 'PENDING')")
    Long countSentTransactions(Long walletId);

    // Receivers should ONLY count SETTLED (SUCCESS) transactions, NOT PENDING
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.receiverWallet.id = :walletId AND t.type = 'P2P_TRANSFER' AND t.status = 'SUCCESS'")
    Long countReceivedTransactions(Long walletId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.senderWallet.id = :walletId AND t.type = 'P2P_TRANSFER' AND t.status IN ('SUCCESS', 'PENDING')")
    BigDecimal sumSentAmount(Long walletId);

    // Receivers should ONLY sum SETTLED (SUCCESS) amounts, NOT PENDING
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.receiverWallet.id = :walletId AND t.type = 'P2P_TRANSFER' AND t.status = 'SUCCESS'")
    BigDecimal sumReceivedAmount(Long walletId);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.senderWallet.id = :walletId AND t.type = 'P2P_TRANSFER' AND t.status IN ('SUCCESS', 'PENDING') AND YEAR(t.createdAt) = YEAR(CURRENT_DATE) AND MONTH(t.createdAt) = MONTH(CURRENT_DATE)")
    Long countSentThisMonth(Long walletId);

    // Receivers should ONLY count SETTLED (SUCCESS) transactions, NOT PENDING
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.receiverWallet.id = :walletId AND t.type = 'P2P_TRANSFER' AND t.status = 'SUCCESS' AND YEAR(t.createdAt) = YEAR(CURRENT_DATE) AND MONTH(t.createdAt) = MONTH(CURRENT_DATE)")
    Long countReceivedThisMonth(Long walletId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.senderWallet.id = :walletId AND t.type = 'P2P_TRANSFER' AND t.status IN ('SUCCESS', 'PENDING') AND YEAR(t.createdAt) = YEAR(CURRENT_DATE) AND MONTH(t.createdAt) = MONTH(CURRENT_DATE)")
    BigDecimal sumSentThisMonth(Long walletId);

    // Receivers should ONLY sum SETTLED (SUCCESS) amounts, NOT PENDING
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.receiverWallet.id = :walletId AND t.type = 'P2P_TRANSFER' AND t.status = 'SUCCESS' AND YEAR(t.createdAt) = YEAR(CURRENT_DATE) AND MONTH(t.createdAt) = MONTH(CURRENT_DATE)")
    BigDecimal sumReceivedThisMonth(Long walletId);

    // Reconciliation queries
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.type = 'P2P_TRANSFER' AND t.status = 'SUCCESS' AND DATE(t.settledAt) = :summaryDate")
    Long countSettledTransactionsForDate(LocalDate summaryDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.type = 'P2P_TRANSFER' AND t.status = 'SUCCESS' AND DATE(t.settledAt) = :summaryDate")
    BigDecimal sumSettledAmountForDate(LocalDate summaryDate);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.type = 'P2P_TRANSFER' AND t.status = 'PENDING' AND DATE(t.createdAt) = :summaryDate")
    Long countPendingTransactionsForDate(LocalDate summaryDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.type = 'P2P_TRANSFER' AND t.status = 'PENDING' AND DATE(t.createdAt) = :summaryDate")
    BigDecimal sumPendingAmountForDate(LocalDate summaryDate);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.type = 'P2P_TRANSFER' AND DATE(t.createdAt) = :summaryDate AND (t.status = 'PENDING' OR t.status = 'SUCCESS')")
    Long countInitiatedTransactionsForDate(LocalDate summaryDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.type = 'P2P_TRANSFER' AND DATE(t.createdAt) = :summaryDate AND (t.status = 'PENDING' OR t.status = 'SUCCESS')")
    BigDecimal sumInitiatedAmountForDate(LocalDate summaryDate);

    // Dashboard queries
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.type = 'P2P_TRANSFER' AND DATE(t.createdAt) = :date AND (t.status = 'PENDING' OR t.status = 'SUCCESS')")
    Long countInitiatedForDate(LocalDate date);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.type = 'P2P_TRANSFER' AND t.status = 'SUCCESS' AND DATE(t.settledAt) = :date")
    Long countSettledForDate(LocalDate date);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.type = 'P2P_TRANSFER' AND t.status = 'SUCCESS' AND DATE(t.settledAt) = :date")
    BigDecimal sumVolumeForDate(LocalDate date);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.status = 'PENDING' AND t.type = 'P2P_TRANSFER'")
    BigDecimal sumPendingSettlementAmount();

    // Admin transaction monitoring queries
    @Query("SELECT t FROM Transaction t WHERE t.type = :type")
    Page<Transaction> findAllByType(String type, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.status = :status")
    Page<Transaction> findAllByStatus(String status, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.amount >= :minAmount AND t.amount <= :maxAmount")
    Page<Transaction> findByAmountRange(BigDecimal minAmount, BigDecimal maxAmount, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE DATE(t.createdAt) >= :startDate AND DATE(t.createdAt) <= :endDate")
    Page<Transaction> findByDateRange(LocalDate startDate, LocalDate endDate, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.type = :type AND t.status = :status")
    Page<Transaction> findByTypeAndStatus(String type, String status, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.type = :type AND t.status = :status AND t.amount >= :minAmount AND t.amount <= :maxAmount")
    Page<Transaction> findByTypeStatusAndAmountRange(String type, String status, BigDecimal minAmount, BigDecimal maxAmount, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.type = :type AND t.status = :status AND DATE(t.createdAt) >= :startDate AND DATE(t.createdAt) <= :endDate")
    Page<Transaction> findByTypeStatusAndDateRange(String type, String status, LocalDate startDate, LocalDate endDate, Pageable pageable);

    // Settlement queue queries
    @Query("SELECT t FROM Transaction t WHERE t.status = 'PENDING' AND t.settlementDueDate < :today ORDER BY t.settlementDueDate ASC")
    List<Transaction> findOverduePendingTransactions(LocalDate today);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.status = 'PENDING' AND t.settlementDueDate < :today")
    Long countOverduePendingTransactions(LocalDate today);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.status = 'PENDING' AND t.settlementDueDate < :today")
    BigDecimal sumOverduePendingAmount(LocalDate today);

    @Query("SELECT t FROM Transaction t WHERE t.status = 'PENDING' AND t.settlementDueDate = :date ORDER BY t.createdAt ASC")
    List<Transaction> findPendingTransactionsDueDate(LocalDate date);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.status = 'PENDING' AND t.settlementDueDate = :date")
    Long countPendingTransactionsDueDate(LocalDate date);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.status = 'PENDING' AND t.settlementDueDate = :date")
    BigDecimal sumPendingAmountDueDate(LocalDate date);

    @Query("SELECT t FROM Transaction t WHERE t.status = 'PENDING' AND t.settlementDueDate > :today ORDER BY t.settlementDueDate ASC")
    List<Transaction> findFuturePendingTransactions(LocalDate today);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.status = 'PENDING' AND t.settlementDueDate > :today")
    Long countFuturePendingTransactions(LocalDate today);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.status = 'PENDING' AND t.settlementDueDate > :today")
    BigDecimal sumFuturePendingAmount(LocalDate today);

    // Enhanced admin transaction monitoring queries with all filter combinations
    @Query("SELECT t FROM Transaction t WHERE t.type = :type AND t.status = :status AND t.amount >= :minAmount AND t.amount <= :maxAmount AND DATE(t.createdAt) >= :startDate AND DATE(t.createdAt) <= :endDate")
    Page<Transaction> findByAllFilters(String type, String status, BigDecimal minAmount, BigDecimal maxAmount, LocalDate startDate, LocalDate endDate, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.status = :status AND t.amount >= :minAmount AND t.amount <= :maxAmount AND DATE(t.createdAt) >= :startDate AND DATE(t.createdAt) <= :endDate")
    Page<Transaction> findByStatusAmountAndDateRange(String status, BigDecimal minAmount, BigDecimal maxAmount, LocalDate startDate, LocalDate endDate, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.type = :type AND t.amount >= :minAmount AND t.amount <= :maxAmount AND DATE(t.createdAt) >= :startDate AND DATE(t.createdAt) <= :endDate")
    Page<Transaction> findByTypeAmountAndDateRange(String type, BigDecimal minAmount, BigDecimal maxAmount, LocalDate startDate, LocalDate endDate, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.type = :type AND t.amount >= :minAmount AND t.amount <= :maxAmount")
    Page<Transaction> findByTypeAndAmountRange(String type, BigDecimal minAmount, BigDecimal maxAmount, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.type = :type AND DATE(t.createdAt) >= :startDate AND DATE(t.createdAt) <= :endDate")
    Page<Transaction> findByTypeAndDateRange(String type, LocalDate startDate, LocalDate endDate, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.status = :status AND t.amount >= :minAmount AND t.amount <= :maxAmount")
    Page<Transaction> findByStatusAndAmountRange(String status, BigDecimal minAmount, BigDecimal maxAmount, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.status = :status AND DATE(t.createdAt) >= :startDate AND DATE(t.createdAt) <= :endDate")
    Page<Transaction> findByStatusAndDateRange(String status, LocalDate startDate, LocalDate endDate, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.amount >= :minAmount AND t.amount <= :maxAmount AND DATE(t.createdAt) >= :startDate AND DATE(t.createdAt) <= :endDate")
    Page<Transaction> findByAmountAndDateRange(BigDecimal minAmount, BigDecimal maxAmount, LocalDate startDate, LocalDate endDate, Pageable pageable);
}
