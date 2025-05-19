package com.iso27001planner.repository;

import com.iso27001planner.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByActorEmail(String email);
}
