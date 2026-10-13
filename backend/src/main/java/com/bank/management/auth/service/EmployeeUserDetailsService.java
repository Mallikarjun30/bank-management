package com.bank.management.auth.service;

import com.bank.management.common.security.EmployeeUserDetails;
import com.bank.management.employee.entity.Employee;
import com.bank.management.employee.enums.EmployeeRole;
import com.bank.management.employee.repository.EmployeeRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;

    public EmployeeUserDetailsService(
            EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new UsernameNotFoundException("Invalid credentials");
        }

        String normalized = identifier.trim();

        Employee employee;

        if (normalized.contains("@")) {
            employee = employeeRepository
                    .findByEmailIgnoreCaseAndRole(
                            normalized,
                            EmployeeRole.OWNER
                    )
                    .orElseThrow(() ->
                            new UsernameNotFoundException(
                                    "Invalid credentials"));
        } else {
            employee = employeeRepository
                    .findByEmployeeId(normalized)
                    .filter(e -> e.getRole() == EmployeeRole.MANAGER
                            || e.getRole() == EmployeeRole.STAFF)
                    .orElseThrow(() ->
                            new UsernameNotFoundException(
                                    "Invalid credentials"));
        }

        return new EmployeeUserDetails(employee);
    }
}   