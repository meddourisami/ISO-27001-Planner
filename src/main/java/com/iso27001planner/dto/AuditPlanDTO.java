package com.iso27001planner.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditPlanDTO {

    private Long id;
    private String title;
    private String description;
    private String type;
    private String scope;
    private String date;
    private String endDate;
    private String status;
    private String auditor;
    private String findings;
    private int nonConformities;
    private int observations;
    private int recommendations;
    private Long companyId;
}
