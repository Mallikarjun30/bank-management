package com.bank.management.common.security;

import com.bank.management.employee.entity.Employee;
import com.bank.management.employee.enums.EmployeeRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public final class EmployeeUserDetails
        implements UserDetails, EmployeePrincipal {

    private final Long internalEmployeeId;
    private final String username;
    private final String passwordHash;
    private final EmployeeRole role;
    private final boolean active;

    public EmployeeUserDetails(Employee employee) {
        this.internalEmployeeId = employee.getId();
        this.role = employee.getRole();
        this.active = employee.getStatus()
                == com.bank.management.employee.enums.EmployeeStatus.ACTIVE;
        this.passwordHash = employee.getPasswordHash();

        this.username = role == EmployeeRole.OWNER
                ? employee.getEmail()
                : employee.getEmployeeId();
    }

    @Override
    public Long getInternalEmployeeId() {
        return internalEmployeeId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority("ROLE_" + role.name())
        );
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}