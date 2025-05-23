package com.iso27001planner.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ControlDTO {
    private String id;
    private String title;
    private String description;
    private String status;
    private String evidence;
    private String lastReview;
    private Long companyId;
}
