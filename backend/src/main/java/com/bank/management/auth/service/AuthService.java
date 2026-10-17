package com.bank.management.auth.service;

import com.bank.management.auth.dto.RefreshRequest;
import com.bank.management.auth.dto.RefreshResponse;
import com.bank.management.auth.dto.SessionHistoryResponse;
import com.bank.management.employee.enums.EmployeeStatus;

import com.bank.management.auth.enums.SessionStatus;
import com.bank.management.auth.enums.SessionTerminationReason;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.bank.management.auth.dto.LoginRequest;
import com.bank.management.auth.dto.LoginResponse;
import com.bank.management.auth.entity.Session;
import com.bank.management.auth.repository.SessionRepository;
import com.bank.management.common.security.EmployeeUserDetails;
import com.bank.management.employee.entity.Employee;
import com.bank.management.employee.repository.EmployeeRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final EmployeeRepository employeeRepository;
    private final SessionRepository sessionRepository;
    private final JwtService jwtService;
    private final long refreshTokenTtlSeconds;

    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            AuthenticationManager authenticationManager,
            EmployeeRepository employeeRepository,
            SessionRepository sessionRepository,
            JwtService jwtService,
            @Value("${JWT_REFRESH_TTL_SECONDS:604800}")
            long refreshTokenTtlSeconds
    ) {
        this.authenticationManager = authenticationManager;
        this.employeeRepository = employeeRepository;
        this.sessionRepository = sessionRepository;
        this.jwtService = jwtService;

        if (refreshTokenTtlSeconds <= 0) {
            throw new IllegalArgumentException(
                    "Refresh-token lifetime must be positive"
            );
        }

        this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.identifier().trim(),
                                request.password()
                        )
                );

        EmployeeUserDetails principal =
                (EmployeeUserDetails) authentication.getPrincipal();

        Employee employee = employeeRepository
                .findById(principal.getInternalEmployeeId())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated employee no longer exists"
                        ));

        String refreshToken = generateRefreshToken();
        String refreshTokenHash = hashToken(refreshToken);

        LocalDateTime now = LocalDateTime.now();

        Session session = new Session(
                employee,
                now,
                refreshTokenHash,
                now.plusSeconds(refreshTokenTtlSeconds)
        );

        session = sessionRepository.save(session);

        String accessToken = jwtService.generateAccessToken(
                employee.getId(),
                employee.getRole().name(),
                session.getSessionId()
        );

        LoginResponse.EmployeeSummary employeeSummary =
                new LoginResponse.EmployeeSummary(
                        employee.getId(),
                        employee.getEmployeeId(),
                        employee.getFullName(),
                        employee.getRole().name()
                );

        return new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.getAccessTokenTtlSeconds(),
                employeeSummary
        );
    }

    @Transactional(readOnly = true)
    public List<SessionHistoryResponse> getSessionHistory(Long employeeId) {
        return sessionRepository
                .findByEmployeeIdOrderByLoginAtDesc(employeeId)
                .stream()
                .map(session -> new SessionHistoryResponse(
                        session.getSessionId(),
                        session.getLoginAt(),
                        session.getEndedAt(),
                        session.getStatus().name(),
                        session.getTerminationReason() == null
                                ? null
                                : session.getTerminationReason().name()
                ))
                .toList();
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public RefreshResponse refresh(RefreshRequest request) {
    String presentedToken = request.refreshToken().trim();

    String presentedTokenHash = hashToken(presentedToken);

    Session session = sessionRepository
            .findByRefreshTokenHash(presentedTokenHash)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid refresh token"
            ));

    LocalDateTime now = LocalDateTime.now();

    // Persist expiration before rejecting an expired refresh token.
    if (session.getStatus() == SessionStatus.ACTIVE
            && session.getRefreshTokenExpiresAt() != null
            && !session.getRefreshTokenExpiresAt().isAfter(now)) {

        session.endSession(
                now,
                SessionStatus.EXPIRED,
                SessionTerminationReason.EXPIRED
        );
        session.revokeRefreshToken(now);
        sessionRepository.save(session);

        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Refresh token is expired or revoked"
        );
    }

    // Validate the remaining session and refresh-token state.
    if (session.getStatus() != SessionStatus.ACTIVE
            || session.getRefreshTokenRevokedAt() != null
            || session.getRefreshTokenExpiresAt() == null) {
        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Refresh token is expired or revoked"
        );
    }

    Employee employee = session.getEmployee();

    // A deactivated employee must not obtain new tokens.
    if (employee.getStatus() != EmployeeStatus.ACTIVE) {
        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Employee account is inactive"
        );
    }

    // Generate a replacement refresh token.
    String newRefreshToken = generateRefreshToken();
    String newRefreshTokenHash = hashToken(newRefreshToken);

    session.rotateRefreshToken(
            newRefreshTokenHash,
            now.plusSeconds(refreshTokenTtlSeconds)
    );

    sessionRepository.save(session);

    // Keep the same session, but issue a new access token.
    String newAccessToken = jwtService.generateAccessToken(
            employee.getId(),
            employee.getRole().name(),
            session.getSessionId()
    );

    return new RefreshResponse(
            newAccessToken,
            newRefreshToken,
            "Bearer",
            jwtService.getAccessTokenTtlSeconds()
    );
}

    private String generateRefreshToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    exception
            );
        }
    }
    @Transactional
public void logout(Long employeeId, String accessToken) {
    final Claims claims;

    try {
        claims = jwtService.parseClaims(accessToken);
    } catch (JwtException | IllegalArgumentException exception) {
        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid or expired access token"
        );
    }

    String subject = claims.getSubject();
    String sessionId = claims.get("sid", String.class);

    if (subject == null || sessionId == null) {
        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid access token claims"
        );
    }

    final Long tokenEmployeeId;

    try {
        tokenEmployeeId = Long.valueOf(subject);
    } catch (NumberFormatException exception) {
        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid access token subject"
        );
    }

    if (!employeeId.equals(tokenEmployeeId)) {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Token does not belong to the authenticated employee"
        );
    }

    Session session = sessionRepository.findBySessionId(sessionId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Session not found"
            ));

    if (!session.getEmployee().getId().equals(employeeId)
            || session.getStatus() != SessionStatus.ACTIVE) {
        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Session is not active"
        );
    }

    LocalDateTime now = LocalDateTime.now();

    session.endSession(
            now,
            SessionStatus.LOGGED_OUT,
            SessionTerminationReason.LOGOUT
    );

    session.revokeRefreshToken(now);

    sessionRepository.save(session);
}
}