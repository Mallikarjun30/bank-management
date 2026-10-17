package com.bank.management.auth.service;

import com.bank.management.auth.dto.RefreshRequest;
import com.bank.management.auth.dto.RefreshResponse;
import com.bank.management.auth.entity.Session;
import com.bank.management.auth.enums.SessionStatus;
import com.bank.management.auth.enums.SessionTerminationReason;
import com.bank.management.auth.repository.SessionRepository;
import com.bank.management.employee.entity.Employee;
import com.bank.management.employee.enums.EmployeeRole;
import com.bank.management.employee.enums.EmployeeStatus;
import com.bank.management.employee.repository.EmployeeRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuthServiceRefreshTest {

    private AuthenticationManager authenticationManager;
    private EmployeeRepository employeeRepository;
    private SessionRepository sessionRepository;
    private JwtService jwtService;

    private AuthService authService;
    private Session session;
    private Employee employee;

    @BeforeEach
    void setUp() {
        authenticationManager = mock(AuthenticationManager.class);
        employeeRepository = mock(EmployeeRepository.class);
        sessionRepository = mock(SessionRepository.class);
        jwtService = mock(JwtService.class);

        authService = new AuthService(
                authenticationManager,
                employeeRepository,
                sessionRepository,
                jwtService,
                604800L
        );

        session = mock(Session.class);
        employee = mock(Employee.class);

        when(session.getEmployee()).thenReturn(employee);
        when(session.getStatus()).thenReturn(SessionStatus.ACTIVE);
        when(session.getRefreshTokenRevokedAt()).thenReturn(null);
        when(session.getRefreshTokenExpiresAt())
                .thenReturn(LocalDateTime.now().plusHours(1));
        when(session.getSessionId()).thenReturn("test-session-id");

        when(employee.getId()).thenReturn(42L);
        when(employee.getRole()).thenReturn(EmployeeRole.OWNER);
        when(employee.getStatus()).thenReturn(EmployeeStatus.ACTIVE);

        when(sessionRepository.findByRefreshTokenHash(anyString()))
                .thenReturn(Optional.of(session));

        when(jwtService.generateAccessToken(
                42L,
                EmployeeRole.OWNER.name(),
                "test-session-id"
        )).thenReturn("new-access-token");

        when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);

        when(sessionRepository.save(any(Session.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void refreshWithValidTokenReturnsReplacementTokens() {
        RefreshResponse response = authService.refresh(
                new RefreshRequest("presented-refresh-token")
        );

        assertNotNull(response);
        assertEquals("new-access-token", response.accessToken());
        assertNotNull(response.refreshToken());
        assertNotEquals("presented-refresh-token", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900L, response.expiresIn());

        verify(session).rotateRefreshToken(
                anyString(),
                any(LocalDateTime.class)
        );
        verify(sessionRepository).save(session);
        verify(jwtService).generateAccessToken(
                42L,
                EmployeeRole.OWNER.name(),
                "test-session-id"
        );
    }

    @Test
    void refreshLooksUpTokenByHashRatherThanRawToken() {
        String rawToken = "presented-refresh-token";

        authService.refresh(new RefreshRequest(rawToken));

        org.mockito.ArgumentCaptor<String> hashCaptor =
                org.mockito.ArgumentCaptor.forClass(String.class);

        verify(sessionRepository).findByRefreshTokenHash(
                hashCaptor.capture()
        );

        assertNotEquals(rawToken, hashCaptor.getValue());
        assertFalse(hashCaptor.getValue().isBlank());
    }

    @Test
    void refreshWithUnknownOrReusedTokenReturns401() {
        when(sessionRepository.findByRefreshTokenHash(anyString()))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.refresh(
                        new RefreshRequest("unknown-or-reused-token")
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Invalid refresh token", exception.getReason());

        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    void refreshWithExpiredTokenReturns401AndEndsSession() {
        when(session.getStatus()).thenReturn(SessionStatus.ACTIVE);
        when(session.getRefreshTokenRevokedAt()).thenReturn(null);
        when(session.getRefreshTokenExpiresAt())
                .thenReturn(LocalDateTime.now().minusMinutes(1));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.refresh(
                        new RefreshRequest("expired-refresh-token")
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals(
                "Refresh token is expired or revoked",
                exception.getReason()
        );

        verify(session).endSession(
                any(LocalDateTime.class),
                eq(SessionStatus.EXPIRED),
                eq(SessionTerminationReason.EXPIRED)
        );
        verify(session).revokeRefreshToken(any(LocalDateTime.class));
        verify(sessionRepository).save(session);
    }

    @Test
    void refreshWithRevokedTokenReturns401() {
        when(session.getRefreshTokenRevokedAt())
                .thenReturn(LocalDateTime.now().minusMinutes(1));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.refresh(
                        new RefreshRequest("revoked-refresh-token")
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());

        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    void refreshWithInactiveSessionReturns401() {
        when(session.getStatus()).thenReturn(SessionStatus.LOGGED_OUT);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.refresh(
                        new RefreshRequest("inactive-session-token")
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());

        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    void refreshForInactiveEmployeeReturns401() {
        when(employee.getStatus()).thenReturn(EmployeeStatus.INACTIVE);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.refresh(
                        new RefreshRequest("active-session-inactive-employee")
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals(
                "Employee account is inactive",
                exception.getReason()
        );

        verify(sessionRepository, never()).save(any(Session.class));
    }
}