package com.bank.management.employee.dto;

public record EmployeeAssignmentRequest(
        Long branchId,
        Long reportingManagerId
) {
}