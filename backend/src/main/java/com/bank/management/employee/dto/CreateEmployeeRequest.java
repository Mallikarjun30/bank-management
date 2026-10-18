package com.bank.management.employee.dto;

import com.bank.management.employee.enums.Gender;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateEmployeeRequest(
        @NotBlank @Size(max = 30)
        String employeeId,

        @Email @Size(max = 255)
        String email,

        @NotBlank @Size(min = 8, max = 72)
        String initialPassword,

        @NotBlank @Size(max = 150)
        String fullName,

        @NotNull @Past
        LocalDate dateOfBirth,

        @NotBlank
        @Pattern(regexp = "\\d+", message = "Identity proof number must contain digits only")
        @Size(max = 50)
        String identityProofNumber,

        @NotBlank
        @Pattern(regexp = "^[0-9+()\\- ]{10,20}$", message = "Invalid phone number format")
        String phoneNumber,

        @NotNull
        Gender gender,

        @NotNull @PastOrPresent
        LocalDate dateOfJoining,

        @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2)
        BigDecimal monthlySalary,

        Long branchId,

        Long reportingManagerId
) {
}