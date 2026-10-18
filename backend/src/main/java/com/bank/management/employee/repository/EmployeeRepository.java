package com.bank.management.employee.repository;

import com.bank.management.employee.entity.Employee;
import com.bank.management.employee.enums.EmployeeRole;
import com.bank.management.employee.enums.EmployeeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    List<Employee> findAllByEmailIgnoreCase(String email);

    Optional<Employee> findByEmployeeId(String employeeId);

    Optional<Employee> findByEmailIgnoreCaseAndRole(
            String email,
            EmployeeRole role
    );

    Optional<Employee> findFirstByRoleAndStatusOrderByIdAsc(
            EmployeeRole role,
            EmployeeStatus status
    );

    boolean existsByEmailAndRole(String email, EmployeeRole role);

    boolean existsByEmployeeId(String employeeId);

    boolean existsByEmployeeIdAndIdNot(String employeeId, Long id);

    boolean existsByIdentityProofNumber(String identityProofNumber);

    boolean existsByIdentityProofNumberAndIdNot(
            String identityProofNumber,
            Long id
    );

    boolean existsByBranch_Id(Long branchId);

    boolean existsByRole(EmployeeRole role);

    long countByRole(EmployeeRole role);

    List<Employee> findAllByRoleInOrderByIdAsc(List<EmployeeRole> roles);

    List<Employee> findAllByReportingManager_IdAndRoleOrderByIdAsc(
            Long reportingManagerId,
            EmployeeRole role
    );

    boolean existsByReportingManager_IdAndRoleAndStatus(
            Long reportingManagerId,
            EmployeeRole role,
            EmployeeStatus status
    );
}