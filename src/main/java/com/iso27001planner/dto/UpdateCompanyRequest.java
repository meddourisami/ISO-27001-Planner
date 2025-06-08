package com.iso27001planner.dto;

import lombok.Data;

@Data
public class UpdateCompanyRequest {
    private Long companyId;
    private String name;
    private String ismsScope;
}
