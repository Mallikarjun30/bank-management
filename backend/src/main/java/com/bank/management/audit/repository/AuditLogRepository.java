package com.bank.management.audit.repository;

import com.bank.management.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByActorId(Long actorId);

    List<AuditLog> findByAction(String action);

    List<AuditLog> findByEntityTypeAndResourceId(
            String entityType,
            String resourceId
    );
}