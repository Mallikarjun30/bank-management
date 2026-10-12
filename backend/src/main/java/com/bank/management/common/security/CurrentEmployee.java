package com.bank.management.common.security;

import com.bank.management.common.exception.ResourceNotFoundException;
import com.bank.management.employee.entity.Employee;
import com.bank.management.employee.repository.EmployeeRepository;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentEmployee {

    private final EmployeeRepository employeeRepository;

    public CurrentEmployee(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    /**
     * Returns the employee associated with the current authenticated principal.
     */
    public Employee get() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationCredentialsNotFoundException(
                    "An authenticated employee is required");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof EmployeePrincipal employeePrincipal)) {
            throw new AuthenticationCredentialsNotFoundException(
                    "The authenticated principal is not an employee principal");
        }

        Long employeeId = employeePrincipal.getInternalEmployeeId();

        if (employeeId == null) {
            throw new AuthenticationCredentialsNotFoundException(
                    "The authenticated principal has no employee ID");
        }

        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "The authenticated employee no longer exists"));
    }

    /**
     * Returns the internal database ID of the current employee.
     */
    public Long getId() {
        return get().getId();
    }
}