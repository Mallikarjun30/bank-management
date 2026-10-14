
package com.bank.management.config;

import com.bank.management.employee.entity.Employee;
import com.bank.management.employee.enums.EmployeeRole;
import com.bank.management.employee.enums.EmployeeStatus;
import com.bank.management.employee.enums.Gender;
import com.bank.management.employee.repository.EmployeeRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

@Configuration
public class OwnerBootstrapConfig {

    @Bean
    CommandLineRunner initializeOwner(
            EmployeeRepository employeeRepository,
            PasswordEncoder passwordEncoder,
            @Value("${OWNER_EMAIL:}") String email,
            @Value("${OWNER_PASSWORD:}") String password,
            @Value("${OWNER_FULL_NAME:}") String fullName,
            @Value("${OWNER_DATE_OF_BIRTH:}") String dateOfBirth,
            @Value("${OWNER_IDENTITY_PROOF_NUMBER:}") String identityProofNumber,
            @Value("${OWNER_PHONE_NUMBER:}") String phoneNumber,
            @Value("${OWNER_GENDER:}") String gender,
            @Value("${OWNER_DATE_OF_JOINING:}") String dateOfJoining,
            @Value("${OWNER_MONTHLY_SALARY:}") String monthlySalary
    ) {
        return args -> {
            long ownerCount =
                    employeeRepository.countByRole(EmployeeRole.OWNER);

            if (ownerCount > 1) {
                throw new IllegalStateException(
                        "Multiple Owner accounts exist. Resolve this before startup."
                );
            }

            if (ownerCount == 1) {
                return;
            }

            requireConfigured("OWNER_EMAIL", email);
            requireConfigured("OWNER_PASSWORD", password);
            requireConfigured("OWNER_FULL_NAME", fullName);
            requireConfigured("OWNER_DATE_OF_BIRTH", dateOfBirth);
            requireConfigured(
                    "OWNER_IDENTITY_PROOF_NUMBER",
                    identityProofNumber
            );
            requireConfigured("OWNER_PHONE_NUMBER", phoneNumber);
            requireConfigured("OWNER_GENDER", gender);
            requireConfigured("OWNER_DATE_OF_JOINING", dateOfJoining);
            requireConfigured("OWNER_MONTHLY_SALARY", monthlySalary);

            if (password.length() < 12) {
                throw new IllegalStateException(
                        "OWNER_PASSWORD must contain at least 12 characters."
                );
            }

            LocalDate dob = LocalDate.parse(dateOfBirth);
            LocalDate joiningDate = LocalDate.parse(dateOfJoining);
            Gender parsedGender = Gender.valueOf(
                    gender.trim().toUpperCase(Locale.ROOT)
            );
            BigDecimal salary = new BigDecimal(monthlySalary);

            if (!dob.isBefore(LocalDate.now())) {
                throw new IllegalStateException(
                        "OWNER_DATE_OF_BIRTH must be in the past."
                );
            }

            if (joiningDate.isAfter(LocalDate.now())) {
                throw new IllegalStateException(
                        "OWNER_DATE_OF_JOINING cannot be in the future."
                );
            }

            if (salary.signum() < 0) {
                throw new IllegalStateException(
                        "OWNER_MONTHLY_SALARY cannot be negative."
                );
            }

            if (employeeRepository.findByEmailIgnoreCaseAndRole(
                    email.trim(), EmployeeRole.OWNER
            ).isPresent()) {
                throw new IllegalStateException(
                        "An Owner with this email exists, but the Owner count is inconsistent."
                );
            }

            if (employeeRepository.existsByIdentityProofNumber(
                    identityProofNumber.trim()
            )) {
                throw new IllegalStateException(
                        "OWNER_IDENTITY_PROOF_NUMBER is already in use."
                );
            }

            Employee owner = new Employee(
                    null,
                    email.trim(),
                    passwordEncoder.encode(password),
                    fullName.trim(),
                    dob,
                    identityProofNumber.trim(),
                    phoneNumber.trim(),
                    parsedGender,
                    joiningDate,
                    salary,
                    EmployeeRole.OWNER,
                    EmployeeStatus.ACTIVE,
                    null,
                    null
            );

            employeeRepository.save(owner);
        };
    }

    private static void requireConfigured(String key, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Missing required configuration: " + key
            );
        }
    }
}
