package com.iso27001planner.dto;

import lombok.Data;

@Data
public class AdminUserCreationRequest {
    private String email;
    private String password;
    private String companyName;
    private String scope;
}
