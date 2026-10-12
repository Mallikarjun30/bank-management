package com.bank.management.common.dto;

import java.time.Instant;

public record ApiSuccessResponse<T>(
        Instant timestamp,
        int status,
        String message,
        T data
) {
}