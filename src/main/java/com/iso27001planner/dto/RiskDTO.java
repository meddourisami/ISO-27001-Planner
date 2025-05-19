package com.iso27001planner.dto;

import lombok.Data;

@Data
public class RiskDTO {
    private String id;
    private String title;
    private String description;
    private String assetId;
    private String threat;
    private String vulnerability;
    private String likelihood;
    private String impact;
    private String severity;
    private String status;
    private String treatment;
    private String controls;
    private String dueDate;
    private String ownerEmail;
    private Long companyId;
}