package com.bank.management.common.security;

import com.bank.management.auth.entity.Session;
import com.bank.management.auth.enums.SessionStatus;
import com.bank.management.auth.repository.SessionRepository;
import com.bank.management.auth.service.JwtService;
import com.bank.management.employee.entity.Employee;
import com.bank.management.employee.enums.EmployeeStatus;
import com.bank.management.employee.repository.EmployeeRepository;

import io.jsonwebtoken.Claims;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final EmployeeRepository employeeRepository;
    private final SessionRepository sessionRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            EmployeeRepository employeeRepository,
            SessionRepository sessionRepository
    ) {
        this.jwtService = jwtService;
        this.employeeRepository = employeeRepository;
        this.sessionRepository = sessionRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7).trim();

        try {
            if (token.isEmpty()
                    || SecurityContextHolder.getContext()
                    .getAuthentication() != null) {
                filterChain.doFilter(request, response);
                return;
            }

            Claims claims = jwtService.parseClaims(token);

            String subject = claims.getSubject();
            String role = claims.get("role", String.class);
            String sessionId = claims.get("sid", String.class);

            if (subject == null || role == null || sessionId == null) {
                filterChain.doFilter(request, response);
                return;
            }

            Long employeeId = Long.valueOf(subject);

            Session session = sessionRepository
                    .findBySessionId(sessionId)
                    .orElse(null);

            if (session == null
                    || session.getStatus() != SessionStatus.ACTIVE
                    || !session.getEmployee().getId().equals(employeeId)) {
                filterChain.doFilter(request, response);
                return;
            }

            Employee employee = employeeRepository.findById(employeeId)
                    .orElse(null);

            if (employee == null
                    || employee.getStatus() != EmployeeStatus.ACTIVE
                    || !employee.getRole().name().equals(role)) {
                filterChain.doFilter(request, response);
                return;
            }

            EmployeeUserDetails principal =
                    new EmployeeUserDetails(employee);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            principal.getAuthorities()
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource()
                            .buildDetails(request)
            );

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

        } catch (io.jsonwebtoken.JwtException
                 | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}