
package com.bank.management.auth.controller;

import com.bank.management.auth.dto.RefreshRequest;

import com.bank.management.auth.dto.PasswordResetRequest;
import com.bank.management.auth.dto.PasswordResetConfirmRequest;
import com.bank.management.auth.service.PasswordResetEmailService;
import com.bank.management.auth.service.PasswordResetService;

import org.springframework.mail.MailException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import com.bank.management.auth.dto.AuthMeResponse;
import com.bank.management.auth.dto.LoginRequest;
import com.bank.management.auth.service.AuthService;
import com.bank.management.common.security.CurrentEmployee;
import com.bank.management.employee.entity.Employee;

import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentEmployee currentEmployee;
    private final PasswordResetService passwordResetService;
    private final PasswordResetEmailService passwordResetEmailService;

    public AuthController(
        AuthService authService,
        CurrentEmployee currentEmployee,
        PasswordResetService passwordResetService,
        PasswordResetEmailService passwordResetEmailService
) {
    this.authService = authService;
    this.currentEmployee = currentEmployee;
    this.passwordResetService = passwordResetService;
    this.passwordResetEmailService = passwordResetEmailService;
}

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request
    ) {
        try {
            return ResponseEntity.ok(authService.login(request));
        } catch (AuthenticationException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "error", "UNAUTHORIZED",
                            "message", "Invalid identifier or password"
                    ));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<AuthMeResponse> me() {
        Employee employee = currentEmployee.get();

        AuthMeResponse response = new AuthMeResponse(
                employee.getId(),
                employee.getEmployeeId(),
                employee.getFullName(),
                employee.getEmail(),
                employee.getRole().name(),
                employee.getStatus().name()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            ) String authorization
    ) {
        if (authorization == null
                || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String accessToken = authorization.substring(7).trim();

        if (accessToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        authService.logout(currentEmployee.getId(), accessToken);

        return ResponseEntity.noContent().build();
    }
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @Valid @RequestBody RefreshRequest request
    ) {
        return ResponseEntity.ok(authService.refresh(request));
    }
    @PostMapping("/password-reset/request")
public ResponseEntity<Map<String, String>> requestPasswordReset(
        @Valid @RequestBody PasswordResetRequest request
) {
    String rawToken =
            passwordResetService.createResetToken(request.email());

    if (rawToken != null) {
        try {
            passwordResetEmailService.sendResetLink(
                    request.email().trim(),
                    rawToken
            );
        } catch (MailException exception) {
            // Do not expose the email account's existence to the caller.
            // Do not log the raw reset token.
            // A production system should also alert operations securely.
        } catch (
                com.bank.management.auth.service.PasswordResetEmailException
                exception
        ) {
            // Keep the public response generic if email delivery fails.
        }
    }

    return ResponseEntity.ok(Map.of(
            "message",
            "If an eligible account exists, password reset instructions will be sent."
    ));
}

@PostMapping("/password-reset/confirm")
public ResponseEntity<Map<String, String>> confirmPasswordReset(
        @Valid @RequestBody PasswordResetConfirmRequest request
) {
    passwordResetService.resetPassword(
            request.token(),
            request.newPassword()
    );

    return ResponseEntity.ok(Map.of(
            "message",
            "Password has been reset successfully."
    ));
}
}
