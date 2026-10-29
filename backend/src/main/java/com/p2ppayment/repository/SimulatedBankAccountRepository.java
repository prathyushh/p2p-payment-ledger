package com.p2ppayment.repository;

import com.p2ppayment.entity.SimulatedBankAccount;
import com.p2ppayment.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SimulatedBankAccountRepository extends JpaRepository<SimulatedBankAccount, Long> {
    List<SimulatedBankAccount> findByUserAndIsActiveTrue(User user);
    
    List<SimulatedBankAccount> findByUser(User user);
    
    Optional<SimulatedBankAccount> findByAccountIdentifier(String accountIdentifier);
    
    Optional<SimulatedBankAccount> findByIdAndUser(Long id, User user);
}