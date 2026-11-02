package com.p2ppayment.repository;

import com.p2ppayment.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    Optional<User> findByEmail(String email);
    Optional<User> findByPhoneNumber(String phoneNumber);

    @Query("SELECT u FROM User u WHERE u.accountLocked = true AND u.lockedUntil <= :now")
    List<User> findLockedAccountsToUnlock(LocalDateTime now);

    @Query("SELECT u FROM User u WHERE u.role = 'ADMIN'")
    List<User> findAllAdminUsers();

    @Query("SELECT u FROM User u WHERE u.role = 'USER' ORDER BY u.createdAt DESC")
    Page<User> findAllUsers(Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.role = 'USER' AND (u.email LIKE %:search% OR u.name LIKE %:search% OR u.phoneNumber LIKE %:search%) ORDER BY u.createdAt DESC")
    Page<User> searchUsers(String search, Pageable pageable);

    // Search for recipient by email or phone (for send money autocomplete)
    @Query("SELECT u FROM User u WHERE u.role = 'USER' AND u.enabled = true AND (LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR u.phoneNumber LIKE CONCAT('%', :query, '%') OR LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<User> searchRecipients(@Param("query") String query);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE users SET failed_login_attempts = failed_login_attempts + 1, last_failed_login = NOW() WHERE id = :id", nativeQuery = true)
    void incrementFailedLoginAttempts(@Param("id") Long id);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE users SET account_locked = true, locked_until = :lockedUntil WHERE id = :id", nativeQuery = true)
    void lockAccount(@Param("id") Long id, @Param("lockedUntil") LocalDateTime lockedUntil);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE users SET failed_login_attempts = 0, last_failed_login = NULL WHERE id = :id", nativeQuery = true)
    void resetFailedLoginAttempts(@Param("id") Long id);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE users SET account_locked = false, locked_until = NULL, failed_login_attempts = 0, last_failed_login = NULL WHERE id = :id", nativeQuery = true)
    void unlockAccount(@Param("id") Long id);

    @Query(value = "SELECT failed_login_attempts FROM users WHERE id = :id", nativeQuery = true)
    int getFailedLoginAttempts(@Param("id") Long id);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'USER' AND DATE(u.createdAt) = :date")
    Long countNewRegistrationsForDate(LocalDate date);
}


