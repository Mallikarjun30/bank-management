package com.bank.management.employee.dto;

import com.bank.management.employee.enums.EmployeeStatus;
import jakarta.validation.constraints.NotNull;

public record EmployeeStatusRequest(
        @NotNull EmployeeStatus status
) {
}