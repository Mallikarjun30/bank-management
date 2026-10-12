package com.bank.management.common.dto;

import com.bank.management.common.enums.ErrorCode;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        ErrorCode errorCode,
        String message,
        String path,
        List<FieldValidationError> fieldErrors
) {
    public ApiErrorResponse {
        fieldErrors = fieldErrors == null
                ? List.of()
                : List.copyOf(fieldErrors);
    }
}