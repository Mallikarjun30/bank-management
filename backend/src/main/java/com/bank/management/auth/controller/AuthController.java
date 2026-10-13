
package com.bank.management.auth.controller;

import com.bank.management.auth.dto.RefreshRequest;

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

    public AuthController(
            AuthService authService,
            CurrentEmployee currentEmployee
    ) {
        this.authService = authService;
        this.currentEmployee = currentEmployee;
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
}
