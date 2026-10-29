package com.p2ppayment.repository;

import com.p2ppayment.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
}
