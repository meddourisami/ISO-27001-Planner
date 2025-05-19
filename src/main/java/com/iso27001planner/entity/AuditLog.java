package com.iso27001planner.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    @Id
    @GeneratedValue
    private Long id;

    private String actorEmail;
    private String actionType;
    private String entityType;
    private String entityId;
    private String description;
    private String ipAddress;
    private LocalDateTime timestamp;
}

