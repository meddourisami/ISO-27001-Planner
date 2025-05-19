package com.iso27001planner.dto;

import lombok.Data;

@Data
public class MFARequest {
    private String email;
    private String code; //code sent by email
}
