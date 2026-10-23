package com.bank.management.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CustomerCreateRequest(
        @NotBlank(message = "Customer name is required")
        @Size(max = 150, message = "Name must not exceed 150 characters")
        String name,

        @NotNull(message = "Date of birth is required")
        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth,

        @NotBlank(message = "Identity proof number is required")
        @Pattern(
                regexp = "[0-9]+",
                message = "Identity proof number must contain digits only"
        )
        @Size(max = 50, message = "Identity proof number must not exceed 50 characters")
        String identityProofNumber,

        @NotBlank(message = "Phone number is required")
        @Pattern(
                regexp = "^[0-9+()\\- ]{10,20}$",
                message = "Invalid phone number format"
        )
        String phoneNumber,

        // Required for Owner; Managers and Staff use their own branch.
        Long branchId
) {
}