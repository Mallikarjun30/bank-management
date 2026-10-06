package com.bank.management.branch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BranchRequest(

        @NotBlank(message = "Branch ID is required")
        @Size(max = 20, message = "Branch ID must not exceed 20 characters")
        String branchId,

        @NotBlank(message = "Branch name is required")
        @Size(max = 100, message = "Branch name must not exceed 100 characters")
        String branchName,

        @NotBlank(message = "Location is required")
        @Size(max = 255, message = "Location must not exceed 255 characters")
        String location
) {
}