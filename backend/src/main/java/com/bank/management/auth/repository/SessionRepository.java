package com.bank.management.auth.repository;

import com.bank.management.auth.entity.Session;
import com.bank.management.auth.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findBySessionId(String sessionId);

    boolean existsBySessionId(String sessionId);

    List<Session> findByEmployeeId(Long employeeId);

    List<Session> findByEmployeeIdAndStatus(
            Long employeeId,
            SessionStatus status
    );
}