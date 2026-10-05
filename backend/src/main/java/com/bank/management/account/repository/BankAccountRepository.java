package com.bank.management.account.repository;

import com.bank.management.account.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    Optional<BankAccount> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    Optional<BankAccount> findByCustomerId(Long customerId);

    boolean existsByCustomerId(Long customerId);
}