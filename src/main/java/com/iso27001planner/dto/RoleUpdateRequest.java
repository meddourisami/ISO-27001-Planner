package com.iso27001planner.dto;

import lombok.Data;

@Data
public class RoleUpdateRequest {
    private String email; // user to update
    private String newRole;
}
