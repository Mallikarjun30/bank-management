package com.bank.management.auth.dto;

public record RefreshResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
}