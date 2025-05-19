package com.iso27001planner.dto;

import lombok.Data;

@Data
public class RegisterMemberRequest {
    private String email;
    private String password;
    private String role; // must be ISMS_USER or lower (no ISMS_ADMIN allowed)
}
