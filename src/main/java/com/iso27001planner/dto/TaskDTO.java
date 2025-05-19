package com.iso27001planner.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskDTO {
    private String id;
    private String title;
    private String description;
    private String assignee;
    private String status;
    private String priority;
    private String dueDate;
    private String category;
    private int progress;
    private String relatedControl;

    private Long riskId;
    private Long companyId;
}
