package com.bank.management.transaction.repository;

import com.bank.management.transaction.entity.BankTransaction;
import com.bank.management.transaction.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {

    Optional<BankTransaction> findByTransactionId(String transactionId);

    boolean existsByTransactionId(String transactionId);

    List<BankTransaction> findByAccountId(Long accountId);

    List<BankTransaction> findByPerformedById(Long employeeId);

    List<BankTransaction> findByBranchId(Long branchId);

    List<BankTransaction> findByAccountCustomerId(Long customerId);

    List<BankTransaction> findByType(TransactionType type);

    List<BankTransaction> findByAmount(BigDecimal amount);
}