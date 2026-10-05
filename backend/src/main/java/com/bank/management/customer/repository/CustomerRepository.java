package com.bank.management.customer.repository;

import com.bank.management.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByIdentityProofNumber(String identityProofNumber);

    boolean existsByIdentityProofNumber(String identityProofNumber);
}