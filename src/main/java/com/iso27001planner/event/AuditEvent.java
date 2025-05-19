package com.iso27001planner.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

@Getter
public class AuditEvent extends ApplicationEvent {

    private final String action;         // e.g. "DELETE_RISK"
    private final String performedBy;    // e.g. user@example.com
    private final String entityType;     // e.g. "Risk"
    private final String entityId;       // e.g. "123"
    private final String description;    // e.g. "Deleted risk: Server Exposure"
    //private final LocalDateTime timestamp;

    public AuditEvent(Object source, String action, String performedBy,
                      String entityType, String entityId, String description) {
        super(source);
        this.action = action;
        this.performedBy = performedBy;
        this.entityType = entityType;
        this.entityId = entityId;
        this.description = description;
        //this.timestamp = LocalDateTime.now();
    }
}
