package com.bank.management.auth.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        EmployeeSummary employee
) {
    public record EmployeeSummary(
            Long id,
            String employeeId,
            String fullName,
            String role
    ) {
    }
}