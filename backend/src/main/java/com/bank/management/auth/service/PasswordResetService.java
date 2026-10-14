
package com.bank.management.auth.service;

import java.util.List;

import com.bank.management.auth.entity.PasswordResetToken;
import com.bank.management.auth.entity.Session;
import com.bank.management.auth.enums.SessionStatus;
import com.bank.management.auth.enums.SessionTerminationReason;
import com.bank.management.auth.repository.PasswordResetTokenRepository;
import com.bank.management.auth.repository.SessionRepository;
import com.bank.management.employee.entity.Employee;
import com.bank.management.employee.enums.EmployeeRole;
import com.bank.management.employee.enums.EmployeeStatus;
import com.bank.management.employee.repository.EmployeeRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class PasswordResetService {

    private final EmployeeRepository employeeRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final long tokenTtlMinutes;

    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
            EmployeeRepository employeeRepository,
            PasswordResetTokenRepository tokenRepository,
            SessionRepository sessionRepository,
            PasswordEncoder passwordEncoder,
            @Value("${PASSWORD_RESET_TTL_MINUTES:15}")
            long tokenTtlMinutes
    ) {
        if (tokenTtlMinutes <= 0) {
            throw new IllegalArgumentException(
                    "Password-reset token lifetime must be positive"
            );
        }

        this.employeeRepository = employeeRepository;
        this.tokenRepository = tokenRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenTtlMinutes = tokenTtlMinutes;
    }

    /**
     * Creates a reset token for an existing employee.
     *
     * Email delivery will be added separately. The raw token is returned
     * internally so a delivery component can send it without persisting it.
     * Do not expose this return value from a public production endpoint.
     */
    
@Transactional
public String createResetToken(String email) {
    if (email == null || email.isBlank()) {
        return null;
    }

    List<Employee> matches =
            employeeRepository.findAllByEmailIgnoreCase(email.trim());

    // Do not select an arbitrary employee if an email is duplicated.
    if (matches.size() != 1) {
        return null;
    }

    Employee employee = matches.get(0);

    if (employee.getStatus() != EmployeeStatus.ACTIVE) {
        return null;
    }

    LocalDateTime now = LocalDateTime.now();

    // Invalidate previous tokens before issuing a replacement.
    tokenRepository.invalidateOutstandingTokens(employee.getId(), now);

    String rawToken = generateToken();

    PasswordResetToken resetToken = new PasswordResetToken(
            employee,
            hashToken(rawToken),
            now.plusMinutes(tokenTtlMinutes)
    );

    tokenRepository.save(resetToken);

    // Internal use only: never return this value from the public API.
    return rawToken;
}


    /**
     * Validates and consumes a reset token, updates the password hash,
     * and invalidates active sessions.
     */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank()) {
            throw invalidToken();
        }

        if (newPassword == null || newPassword.length() < 12) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password must contain at least 12 characters"
            );
        }

        String tokenHash = hashToken(rawToken.trim());

        PasswordResetToken resetToken = tokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(this::invalidToken);

        LocalDateTime now = LocalDateTime.now();

        if (resetToken.getUsedAt() != null
                || !resetToken.getExpiresAt().isAfter(now)) {
            throw invalidToken();
        }

        Employee employee = resetToken.getEmployee();

        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw invalidToken();
        }

        // Update the password using the application's BCrypt encoder.
        employee.setPasswordHash(passwordEncoder.encode(newPassword));
        employeeRepository.save(employee);

        // Mark the token as consumed in the same transaction.
        resetToken.consume(now);
        tokenRepository.save(resetToken);

        // Invalidate every active session so old refresh tokens cannot
        // be used to obtain new access tokens after the password change.
        for (Session session :
                sessionRepository.findByEmployeeIdAndStatus(
                        employee.getId(),
                        SessionStatus.ACTIVE
                )) {

            session.endSession(
                    now,
                    SessionStatus.LOGGED_OUT,
                    SessionTerminationReason.LOGOUT
            );

            session.revokeRefreshToken(now);
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

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

    private ResponseStatusException invalidToken() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid or expired password-reset token"
        );
    }
}
