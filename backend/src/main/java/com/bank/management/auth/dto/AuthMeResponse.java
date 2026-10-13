package com.bank.management.auth.dto;

public record AuthMeResponse(
        Long id,
        String employeeId,
        String fullName,
        String email,
        String role,
        String status
) {}