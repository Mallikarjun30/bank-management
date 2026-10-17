package com.bank.management.auth.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import com.bank.management.auth.entity.Session;
import com.bank.management.auth.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findBySessionId(String sessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Session> findByRefreshTokenHash(String refreshTokenHash);

    boolean existsBySessionId(String sessionId);

    List<Session> findByEmployeeIdOrderByLoginAtDesc(Long employeeId);

    List<Session> findByEmployeeIdAndStatus(
            Long employeeId,
            SessionStatus status
    );
}