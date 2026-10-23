package com.bank.management.customer.dto;

import com.bank.management.customer.entity.Customer;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CustomerResponse(
        Long id,
        String name,
        LocalDate dateOfBirth,
        String identityProofNumber,
        String phoneNumber,
        Long branchId,
        String branchCode,
        String branchName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getDateOfBirth(),
                customer.getIdentityProofNumber(),
                customer.getPhoneNumber(),
                customer.getBranch().getId(),
                customer.getBranch().getBranchId(),
                customer.getBranch().getBranchName(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }
}