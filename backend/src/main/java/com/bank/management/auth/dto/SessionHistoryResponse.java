package com.bank.management.auth.dto;

import java.time.LocalDateTime;

public record SessionHistoryResponse(
        String sessionId,
        LocalDateTime loginAt,
        LocalDateTime endedAt,
        String status,
        String terminationReason
) {
}