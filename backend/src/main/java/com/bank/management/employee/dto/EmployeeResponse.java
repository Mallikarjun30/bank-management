package com.bank.management.employee.dto;

import com.bank.management.employee.entity.Employee;
import com.bank.management.employee.enums.EmployeeRole;
import com.bank.management.employee.enums.EmployeeStatus;
import com.bank.management.employee.enums.Gender;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EmployeeResponse(
        Long id,
        String employeeId,
        String email,
        String fullName,
        LocalDate dateOfBirth,
        String identityProofNumber,
        String phoneNumber,
        Gender gender,
        LocalDate dateOfJoining,
        BigDecimal monthlySalary,
        EmployeeRole role,
        EmployeeStatus status,
        Long branchId,
        String branchCode,
        Long reportingManagerId,
        String reportingManagerEmployeeId
) {
    public static EmployeeResponse from(Employee employee, boolean includeSalary) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getEmployeeId(),
                employee.getEmail(),
                employee.getFullName(),
                employee.getDateOfBirth(),
                employee.getIdentityProofNumber(),
                employee.getPhoneNumber(),
                employee.getGender(),
                employee.getDateOfJoining(),
                includeSalary ? employee.getMonthlySalary() : null,
                employee.getRole(),
                employee.getStatus(),
                employee.getBranch() == null ? null : employee.getBranch().getId(),
                employee.getBranch() == null ? null : employee.getBranch().getBranchId(),
                employee.getReportingManager() == null ? null : employee.getReportingManager().getId(),
                employee.getReportingManager() == null
                        ? null : employee.getReportingManager().getEmployeeId()
        );
    }
}