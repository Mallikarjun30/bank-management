package com.bank.management.customer.repository;

import com.bank.management.customer.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByIdentityProofNumber(String identityProofNumber);

    boolean existsByIdentityProofNumberAndIdNot(
            String identityProofNumber,
            Long id
    );

    @Query("""
            SELECT c FROM Customer c
            WHERE (:branchId IS NULL OR c.branch.id = :branchId)
              AND (
                    :query IS NULL
                    OR LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR c.phoneNumber LIKE CONCAT('%', :query, '%')
                    OR c.identityProofNumber LIKE CONCAT('%', :query, '%')
                  )
            """)
    Page<Customer> searchCustomers(
            @Param("branchId") Long branchId,
            @Param("query") String query,
            Pageable pageable
    );
}
