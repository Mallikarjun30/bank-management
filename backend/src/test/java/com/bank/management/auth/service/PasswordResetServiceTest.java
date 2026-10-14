package com.bank.management.auth.service;

import com.bank.management.auth.entity.PasswordResetToken;
import com.bank.management.auth.entity.Session;
import com.bank.management.auth.enums.SessionStatus;
import com.bank.management.auth.enums.SessionTerminationReason;
import com.bank.management.auth.repository.PasswordResetTokenRepository;
import com.bank.management.auth.repository.SessionRepository;
import com.bank.management.employee.entity.Employee;
import com.bank.management.employee.enums.EmployeeStatus;
import com.bank.management.employee.repository.EmployeeRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PasswordResetServiceTest {

    private EmployeeRepository employeeRepository;
    private PasswordResetTokenRepository tokenRepository;
    private SessionRepository sessionRepository;
    private PasswordEncoder passwordEncoder;
    private PasswordResetService passwordResetService;

    private Employee employee;
    private PasswordResetToken resetToken;
    private Session session;

    @BeforeEach
    void setUp() {
        employeeRepository = mock(EmployeeRepository.class);
        tokenRepository = mock(PasswordResetTokenRepository.class);
        sessionRepository = mock(SessionRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);

        passwordResetService = new PasswordResetService(
                employeeRepository,
                tokenRepository,
                sessionRepository,
                passwordEncoder,
                15L
        );

        employee = mock(Employee.class);
        resetToken = mock(PasswordResetToken.class);
        session = mock(Session.class);

        when(employee.getId()).thenReturn(42L);
        when(employee.getStatus()).thenReturn(EmployeeStatus.ACTIVE);

        when(resetToken.getEmployee()).thenReturn(employee);
        when(resetToken.getUsedAt()).thenReturn(null);
        when(resetToken.getExpiresAt())
                .thenReturn(LocalDateTime.now().plusMinutes(10));

        when(tokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(employeeRepository.save(any(Employee.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createResetTokenStoresOnlyTokenHash() {
        when(employeeRepository.findAllByEmailIgnoreCase(
                "owner@bank.local"
        )).thenReturn(List.of(employee));

        String rawToken =
                passwordResetService.createResetToken("owner@bank.local");

        assertNotNull(rawToken);
        assertFalse(rawToken.isBlank());

        var captor =
                org.mockito.ArgumentCaptor.forClass(PasswordResetToken.class);

        verify(tokenRepository).save(captor.capture());

        PasswordResetToken savedToken = captor.getValue();

        assertNotNull(savedToken.getTokenHash());
        assertNotEquals(rawToken, savedToken.getTokenHash());
        assertEquals(42L, savedToken.getEmployee().getId());

        verify(tokenRepository).invalidateOutstandingTokens(
                eq(42L),
                any(LocalDateTime.class)
        );
    }

    @Test
    void createResetTokenForUnknownEmailReturnsNull() {
        when(employeeRepository.findAllByEmailIgnoreCase(
                "unknown@bank.local"
        )).thenReturn(List.of());

        String result =
                passwordResetService.createResetToken("unknown@bank.local");

        assertNull(result);

        verify(tokenRepository, never())
                .save(any(PasswordResetToken.class));
    }

    @Test
    void resetPasswordWithUnknownTokenReturnsBadRequest() {
        when(tokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> passwordResetService.resetPassword(
                        "invalid-token",
                        "NewStrongPassword123!"
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verify(employeeRepository, never()).save(any(Employee.class));
        verify(sessionRepository, never())
                .findByEmployeeIdAndStatus(anyLong(), any(SessionStatus.class));
    }

    @Test
    void resetPasswordWithExpiredTokenIsRejected() {
        when(resetToken.getExpiresAt())
                .thenReturn(LocalDateTime.now().minusMinutes(1));

        when(tokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(resetToken));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> passwordResetService.resetPassword(
                        "expired-token",
                        "NewStrongPassword123!"
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verify(employeeRepository, never()).save(any(Employee.class));
        verify(sessionRepository, never())
                .findByEmployeeIdAndStatus(anyLong(), any(SessionStatus.class));
    }

    @Test
    void resetPasswordWithWeakPasswordIsRejected() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> passwordResetService.resetPassword(
                        "some-token",
                        "short"
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verify(tokenRepository, never()).findByTokenHash(anyString());
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void successfulResetUpdatesPasswordAndInvalidatesSessions() {
        when(tokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(resetToken));

        when(passwordEncoder.encode("NewStrongPassword123!"))
                .thenReturn("new-bcrypt-hash");

        when(sessionRepository.findByEmployeeIdAndStatus(
                42L,
                SessionStatus.ACTIVE
        )).thenReturn(List.of(session));

        passwordResetService.resetPassword(
                "valid-reset-token",
                "NewStrongPassword123!"
        );

        verify(passwordEncoder).encode("NewStrongPassword123!");
        verify(employee).setPasswordHash("new-bcrypt-hash");
        verify(employeeRepository).save(employee);

        verify(resetToken).consume(any(LocalDateTime.class));
        verify(tokenRepository).save(resetToken);

        verify(session).endSession(
                any(LocalDateTime.class),
                eq(SessionStatus.LOGGED_OUT),
                eq(SessionTerminationReason.LOGOUT)
        );

        verify(session).revokeRefreshToken(any(LocalDateTime.class));
    }

    @Test
    void resetPasswordForInactiveEmployeeIsRejected() {
        when(employee.getStatus()).thenReturn(EmployeeStatus.INACTIVE);

        when(tokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(resetToken));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> passwordResetService.resetPassword(
                        "token-for-inactive-employee",
                        "NewStrongPassword123!"
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verify(employeeRepository, never()).save(any(Employee.class));
        verify(sessionRepository, never())
                .findByEmployeeIdAndStatus(anyLong(), any(SessionStatus.class));
    }

    @Test
    void resetPasswordWithAlreadyUsedTokenIsRejected() {
        when(resetToken.getUsedAt())
                .thenReturn(LocalDateTime.now().minusMinutes(1));

        when(tokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(resetToken));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> passwordResetService.resetPassword(
                        "already-used-token",
                        "NewStrongPassword123!"
                )
       );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

       verify(employeeRepository, never()).save(any(Employee.class));
       verify(sessionRepository, never())
                .findByEmployeeIdAndStatus(anyLong(), any(SessionStatus.class));
    }
}