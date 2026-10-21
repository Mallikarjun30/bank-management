package com.bank.management.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.bank.management.common.exception.ResourceNotFoundException;
import com.bank.management.employee.entity.Employee;
import com.bank.management.employee.repository.EmployeeRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

class CurrentEmployeeTest {

    private EmployeeRepository employeeRepository;
    private CurrentEmployee currentEmployee;

    @BeforeEach
    void setUp() {
        employeeRepository = mock(EmployeeRepository.class);
        currentEmployee = new CurrentEmployee(employeeRepository);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsMissingAuthentication() {
        assertThrows(
                AuthenticationCredentialsNotFoundException.class,
                () -> currentEmployee.get()
        );

        verifyNoInteractions(employeeRepository);
    }

    @Test
    void rejectsAnonymousAuthentication() {
        AnonymousAuthenticationToken anonymous =
                new AnonymousAuthenticationToken(
                        "test-key",
                        "anonymousUser",
                        AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")
                );

        SecurityContextHolder.getContext().setAuthentication(anonymous);

        assertThrows(
                AuthenticationCredentialsNotFoundException.class,
                () -> currentEmployee.get()
        );

        verifyNoInteractions(employeeRepository);
    }

    @Test
    void rejectsPrincipalThatIsNotAnEmployeePrincipal() {
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        "ordinary-user",
                        null,
                        AuthorityUtils.createAuthorityList("ROLE_STAFF")
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThrows(
                AuthenticationCredentialsNotFoundException.class,
                () -> currentEmployee.get()
        );

        verifyNoInteractions(employeeRepository);
    }

    @Test
    void returnsEmployeeIdentifiedByAuthenticatedPrincipal() {
        EmployeeUserDetails principal = mock(EmployeeUserDetails.class);
        Employee employee = mock(Employee.class);

        when(principal.getInternalEmployeeId()).thenReturn(42L);
        when(employeeRepository.findById(42L))
                .thenReturn(Optional.of(employee));

        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        AuthorityUtils.createAuthorityList("ROLE_STAFF")
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        Employee result = currentEmployee.get();

        assertEquals(employee, result);
        verify(employeeRepository).findById(42L);
    }

    @Test
    void rejectsPrincipalWithoutInternalEmployeeId() {
        EmployeeUserDetails principal = mock(EmployeeUserDetails.class);
        when(principal.getInternalEmployeeId()).thenReturn(null);

        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        AuthorityUtils.createAuthorityList("ROLE_STAFF")
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThrows(
                AuthenticationCredentialsNotFoundException.class,
                () -> currentEmployee.get()
        );

        verifyNoInteractions(employeeRepository);
    }

    @Test
    void rejectsEmployeeThatNoLongerExists() {
        EmployeeUserDetails principal = mock(EmployeeUserDetails.class);
        when(principal.getInternalEmployeeId()).thenReturn(42L);
        when(employeeRepository.findById(42L)).thenReturn(Optional.empty());

        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        AuthorityUtils.createAuthorityList("ROLE_STAFF")
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThrows(
                ResourceNotFoundException.class,
                () -> currentEmployee.get()
        );

        verify(employeeRepository).findById(42L);
    }
}
