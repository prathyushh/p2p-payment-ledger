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
    @Query("SELECT t FROM Transaction t WHERE t.receiverWallet.id = :walletId ORDER BY t.createdAt DESC")
    Page<Transaction> findByReceiverWallet(Long walletId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.receiverWallet.id = :walletId AND t.type = :type ORDER BY t.createdAt DESC")
    Page<Transaction> findByReceiverWalletAndType(Long walletId, Transaction.Type type, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.receiverWallet.id = :walletId AND t.status = :status ORDER BY t.createdAt DESC")
    Page<Transaction> findByReceiverWalletAndStatus(Long walletId, Transaction.Status status, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.receiverWallet.id = :walletId AND t.createdAt >= :startDate AND t.createdAt < :endDate ORDER BY t.createdAt DESC")
    Page<Transaction> findByReceiverWalletAndDateRange(Long walletId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    // Mini-statement: last 10 transactions (sent + received combined)
    @Query("SELECT t FROM Transaction t WHERE (t.senderWallet.id = :walletId OR t.receiverWallet.id = :walletId) ORDER BY t.createdAt DESC")
    List<Transaction> findMiniStatement(Long walletId, Pageable pageable);

    // Duplicate detection query: same sender, receiver, amount within 2-minute window
    @Query("SELECT t FROM Transaction t WHERE t.senderWallet.id = :senderWalletId AND t.receiverWallet.id = :receiverWalletId AND t.amount = :amount AND t.createdAt > :twoMinutesAgo AND t.status IN ('PENDING', 'SUCCESS')")
    List<Transaction> findRecentDuplicates(Long senderWalletId, Long receiverWalletId, BigDecimal amount, LocalDateTime twoMinutesAgo);

    // Stats queries
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.senderWallet.id = :walletId AND t.type = 'P2P_TRANSFER' AND t.status IN ('SUCCESS', 'PENDING')")
    Long countSentTransactions(Long walletId);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.receiverWallet.id = :walletId AND t.type = 'P2P_TRANSFER' AND t.status IN ('SUCCESS', 'PENDING')")
    Long countReceivedTransactions(Long walletId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.senderWallet.id = :walletId AND t.type = 'P2P_TRANSFER' AND t.status IN ('SUCCESS', 'PENDING')")
    BigDecimal sumSentAmount(Long walletId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.receiverWallet.id = :walletId AND t.type = 'P2P_TRANSFER' AND t.status IN ('SUCCESS', 'PENDING')")
    BigDecimal sumReceivedAmount(Long walletId);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.senderWallet.id = :walletId AND t.type = 'P2P_TRANSFER' AND t.status IN ('SUCCESS', 'PENDING') AND YEAR(t.createdAt) = YEAR(CURRENT_DATE) AND MONTH(t.createdAt) = MONTH(CURRENT_DATE)")
    Long countSentThisMonth(Long walletId);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.receiverWallet.id = :walletId AND t.type = 'P2P_TRANSFER' AND t.status IN ('SUCCESS', 'PENDING') AND YEAR(t.createdAt) = YEAR(CURRENT_DATE) AND MONTH(t.createdAt) = MONTH(CURRENT_DATE)")
    Long countReceivedThisMonth(Long walletId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.senderWallet.id = :walletId AND t.type = 'P2P_TRANSFER' AND t.status IN ('SUCCESS', 'PENDING') AND YEAR(t.createdAt) = YEAR(CURRENT_DATE) AND MONTH(t.createdAt) = MONTH(CURRENT_DATE)")
    BigDecimal sumSentThisMonth(Long walletId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.receiverWallet.id = :walletId AND t.type = 'P2P_TRANSFER' AND t.status IN ('SUCCESS', 'PENDING') AND YEAR(t.createdAt) = YEAR(CURRENT_DATE) AND MONTH(t.createdAt) = MONTH(CURRENT_DATE)")
    BigDecimal sumReceivedThisMonth(Long walletId);
}