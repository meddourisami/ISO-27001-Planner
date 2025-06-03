package com.iso27001planner.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String email;
    private String fullName;
}
