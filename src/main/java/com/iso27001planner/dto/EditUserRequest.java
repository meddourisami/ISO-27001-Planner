package com.iso27001planner.dto;

import lombok.Data;

@Data
public class EditUserRequest {
    private String targetEmail;   // 🔄 instead of userId
    private String email;
    private String fullName;
    private String role; // e.g., ISMS_USER, AUDITOR
}
