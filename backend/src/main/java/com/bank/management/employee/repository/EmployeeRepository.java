package com.bank.management.employee.repository;

import java.util.List;

import com.bank.management.employee.entity.Employee;
import com.bank.management.employee.enums.EmployeeRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    List<Employee> findAllByEmailIgnoreCase(String email);

    Optional<Employee> findByEmployeeId(String employeeId);

    Optional<Employee> findByEmailIgnoreCaseAndRole(
            String email,
            EmployeeRole role
    );

    boolean existsByEmailAndRole(String email, EmployeeRole role);

    boolean existsByEmployeeId(String employeeId);

    boolean existsByIdentityProofNumber(String identityProofNumber);

    boolean existsByBranch_Id(Long branchId);

    boolean existsByRole(EmployeeRole role);

    long countByRole(EmployeeRole role);
}