package com.bank.management.branch.repository;

import com.bank.management.branch.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Long> {

    Optional<Branch> findByBranchId(String branchId);

    boolean existsByBranchId(String branchId);

    boolean existsByBranchIdAndIdNot(String branchId, Long id);
}