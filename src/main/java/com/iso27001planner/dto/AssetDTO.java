package com.iso27001planner.dto;

import lombok.Data;

import java.util.List;

@Data
public class AssetDTO {
    private String id;
    private String name;
    private String description;
    private String type;
    private String classification;
    private String location;
    private String status;
    private String value;
    private String vulnerabilities;
    private String controls;
    private String lastReview;
    private List<String> relatedRisks;
    private String ownerEmail;
    private Long companyId;
}
