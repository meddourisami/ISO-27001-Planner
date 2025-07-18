package com.iso27001planner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentDTO {
    private Long id;
    private String title;
    private String description;
    private String type;
    private String status;
    private String version;
    private String owner;
    private String approver;
    private String approvalDate;
    private String reviewDate;
    private String content;
    private String relatedControls;

    private Long companyId;

}
