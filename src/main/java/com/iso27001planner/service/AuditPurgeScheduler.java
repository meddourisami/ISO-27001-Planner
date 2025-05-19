package com.iso27001planner.service;

import com.iso27001planner.entity.AuditLog;
import com.iso27001planner.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AuditPurgeScheduler {

    private final AuditEventRepository repository;

    @Scheduled(cron = "0 0 3 * * ?") // Every day at 3 AM
    public void purgeOldLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(356);
        List<AuditLog> old = repository.findAll().stream()
                .filter(log -> log.getTimestamp().isBefore(cutoff))
                .toList();

        if (!old.isEmpty()) {
            repository.deleteAll(old);
            System.out.println("🧹 Purged " + old.size() + " old audit logs.");
        }
    }
}
