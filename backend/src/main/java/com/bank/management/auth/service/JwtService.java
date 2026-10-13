
package com.bank.management.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTokenTtlSeconds;

    public JwtService(
            @Value("${JWT_SECRET_BASE64}") String secretBase64,
            @Value("${JWT_ACCESS_TTL_SECONDS:900}")
            long accessTokenTtlSeconds
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(secretBase64);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);

        if (accessTokenTtlSeconds <= 0) {
            throw new IllegalArgumentException(
                    "JWT access-token lifetime must be positive"
            );
        }

        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
    }

    public String generateAccessToken(
            Long employeeId,
            String role,
            String sessionId
    ) {
        if (employeeId == null || role == null || sessionId == null) {
            throw new IllegalArgumentException(
                    "Employee ID, role, and session ID are required"
            );
        }

        Instant now = Instant.now();

        return Jwts.builder()
                .subject(employeeId.toString())
                .claim("role", role)
                .claim("sid", sessionId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(
                        now.plusSeconds(accessTokenTtlSeconds)
                ))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            Claims claims = parseClaims(token);

            return claims.getSubject() != null
                    && claims.get("role", String.class) != null
                    && claims.get("sid", String.class) != null;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }
}
