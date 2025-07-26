package com.iso27001planner.dto;

import com.iso27001planner.entity.*;
import lombok.Data;

import java.util.List;

@Data
public class BackupDataDTO {
    private Company company;
    private List<User> users;
    private List<Document> documents;
    private List<DocumentVersion> documentVersions;
    private List<Control> controls;
    private List<Risk> risks;
    private List<NonConformity> nonConformities;
    private List<Task> tasks;
    private List<Training> trainings;
    private List<Employee> employees;
    private List<Asset> assets;
    private List<AuditPlan> auditPlans;
    private List<AuditLog> auditLogs;
    private List<Notification> notifications;
}
