package com.bank.management.common.dto;

public record FieldValidationError(
        String field,
        String message
) {
}