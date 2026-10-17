package com.bank.management.auth.service;

import com.bank.management.auth.dto.LoginRequest;
import com.bank.management.auth.dto.LoginResponse;
import com.bank.management.auth.dto.SessionHistoryResponse;
import com.bank.management.auth.enums.SessionTerminationReason;
import com.bank.management.auth.repository.SessionRepository;
import com.bank.management.employee.entity.Employee;
import com.bank.management.employee.enums.EmployeeRole;
import com.bank.management.employee.enums.EmployeeStatus;
import com.bank.management.employee.repository.EmployeeRepository;
import com.bank.management.auth.entity.Session;
import com.bank.management.auth.enums.SessionStatus;
import com.bank.management.common.security.EmployeeUserDetails;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceLoginLogoutTest {

    private AuthenticationManager authenticationManager;
    private EmployeeRepository employeeRepository;
    private SessionRepository sessionRepository;
    private JwtService jwtService;
    private AuthService authService;

    private Employee employee;
    private Session session;

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

        employee = mock(Employee.class);
        session = mock(Session.class);

        when(employee.getId()).thenReturn(42L);
        when(employee.getEmployeeId()).thenReturn("EMP-0042");
        when(employee.getEmail()).thenReturn("owner@bank.local");
        when(employee.getFullName()).thenReturn("Bank Owner");
        when(employee.getPasswordHash()).thenReturn("stored-password-hash");
        when(employee.getRole()).thenReturn(EmployeeRole.OWNER);
        when(employee.getStatus()).thenReturn(EmployeeStatus.ACTIVE);

        when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);

        when(sessionRepository.save(any(Session.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void loginWithValidCredentialsReturnsTokensAndEmployeeSummary() {
        Authentication authentication = mock(Authentication.class);

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(authentication);

        EmployeeUserDetails principal = new EmployeeUserDetails(employee);

        when(authentication.getPrincipal()).thenReturn(principal);

        when(employeeRepository.findById(42L))
                .thenReturn(Optional.of(employee));

        when(jwtService.generateAccessToken(
                eq(42L), eq("OWNER"), nullable(String.class)
        )).thenReturn("test-access-token");

        LoginResponse response = authService.login(
                new LoginRequest("owner@bank.local", "test-password")
        );

        assertNotNull(response);
        assertEquals("test-access-token", response.accessToken());
        assertNotNull(response.refreshToken());
        assertFalse(response.refreshToken().isBlank());
        assertNotEquals("test-password", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900L, response.expiresIn());

        assertEquals(42L, response.employee().id());
        assertEquals("EMP-0042", response.employee().employeeId());
        assertEquals("Bank Owner", response.employee().fullName());
        assertEquals("OWNER", response.employee().role());

        verify(authenticationManager).authenticate(any(Authentication.class));
        verify(sessionRepository).save(any(Session.class));
    }

    @Test
    void loginStoresRefreshTokenHashInsteadOfRawToken() {
        Authentication authentication = mock(Authentication.class);

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(authentication);

        EmployeeUserDetails principal = new EmployeeUserDetails(employee);

        when(authentication.getPrincipal()).thenReturn(principal);

        when(employeeRepository.findById(42L))
                .thenReturn(Optional.of(employee));

        when(jwtService.generateAccessToken(
                eq(42L), eq("OWNER"), nullable(String.class)
        )).thenReturn("test-access-token");

        LoginResponse response = authService.login(
                new LoginRequest("owner@bank.local", "test-password")
        );

        var captor = org.mockito.ArgumentCaptor.forClass(Session.class);

        verify(sessionRepository).save(captor.capture());

        Session savedSession = captor.getValue();

        assertNotNull(savedSession.getRefreshTokenHash());
        assertNotEquals(
                response.refreshToken(),
                savedSession.getRefreshTokenHash()
        );
    }


    @Test
    void getSessionHistoryReturnsSessionMetadataForEmployee() {
        LocalDateTime loginAt = LocalDateTime.of(2026, 10, 17, 10, 0);

        when(sessionRepository.findByEmployeeIdOrderByLoginAtDesc(42L))
                .thenReturn(List.of(session));
        when(session.getSessionId()).thenReturn("test-session-id");
        when(session.getLoginAt()).thenReturn(loginAt);
        when(session.getEndedAt()).thenReturn(null);
        when(session.getStatus()).thenReturn(SessionStatus.ACTIVE);
        when(session.getTerminationReason()).thenReturn(null);

        List<SessionHistoryResponse> response =
                authService.getSessionHistory(42L);

        assertEquals(1, response.size());
        assertEquals("test-session-id", response.get(0).sessionId());
        assertEquals(loginAt, response.get(0).loginAt());
        assertNull(response.get(0).endedAt());
        assertEquals("ACTIVE", response.get(0).status());
        assertNull(response.get(0).terminationReason());

        verify(sessionRepository)
                .findByEmployeeIdOrderByLoginAtDesc(42L);
    }

    @Test
    void logoutWithValidTokenEndsSessionAndRevokesRefreshToken() {
        Claims claims = mock(Claims.class);

        when(jwtService.parseClaims("test-access-token"))
                .thenReturn(claims);

        when(claims.getSubject()).thenReturn("42");
        when(claims.get("sid", String.class))
                .thenReturn("test-session-id");

        when(sessionRepository.findBySessionId("test-session-id"))
                .thenReturn(Optional.of(session));

        when(session.getEmployee()).thenReturn(employee);
        when(session.getStatus()).thenReturn(SessionStatus.ACTIVE);

        authService.logout(42L, "test-access-token");

        verify(session).endSession(
                any(LocalDateTime.class),
                eq(SessionStatus.LOGGED_OUT),
                eq(SessionTerminationReason.LOGOUT)
        );

        verify(session).revokeRefreshToken(any(LocalDateTime.class));
        verify(sessionRepository).save(session);
    }

    @Test
    void logoutRejectsTokenBelongingToAnotherEmployee() {
        Claims claims = mock(Claims.class);

        when(jwtService.parseClaims("test-access-token"))
                .thenReturn(claims);

        when(claims.getSubject()).thenReturn("42");
        when(claims.get("sid", String.class))
                .thenReturn("test-session-id");

        assertThrows(ResponseStatusException.class, () ->
                authService.logout(99L, "test-access-token")
        );

        try {
            authService.logout(99L, "test-access-token");
            fail("Expected logout to reject another employee's token");
        } catch (ResponseStatusException exception) {
            assertEquals(
                    HttpStatus.FORBIDDEN,
                    exception.getStatusCode()
            );
        }

        verify(sessionRepository, never())
                .findBySessionId(anyString());
    }
}
