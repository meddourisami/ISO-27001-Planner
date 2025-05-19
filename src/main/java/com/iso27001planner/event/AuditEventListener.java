package com.iso27001planner.event;

import com.iso27001planner.entity.AuditLog;
import com.iso27001planner.repository.AuditEventRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final AuditEventRepository repository;
    private final HttpServletRequest request;

    @EventListener
    public void handleAuditEvent(AuditEvent event) {
        String ip = request.getRemoteAddr();

        AuditLog log = AuditLog.builder()
                .actionType(event.getAction())
                .actorEmail(event.getPerformedBy())
                .entityType(event.getEntityType())
                .entityId(event.getEntityId())
                .description(event.getDescription())
                .timestamp(LocalDateTime.now())
                .ipAddress(ip)
                //.timestamp(event.getTimestamp())
                .build();

        repository.save(log);

        System.out.println("AUDIT: " + event.getPerformedBy() + " → " + event.getAction());
    }
}
