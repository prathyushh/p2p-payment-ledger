package com.p2ppayment.repository;

import com.p2ppayment.entity.LoginAttempt;
import com.p2ppayment.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    @Query("SELECT la FROM LoginAttempt la WHERE la.user = :user AND la.success = false " +
           "AND la.attemptTime > :since ORDER BY la.attemptTime DESC")
    List<LoginAttempt> findRecentFailedAttempts(User user, LocalDateTime since);

    @Query("SELECT COUNT(la) FROM LoginAttempt la WHERE la.user = :user AND la.success = false " +
           "AND la.attemptTime > :since")
    long countRecentFailedAttempts(User user, LocalDateTime since);
}