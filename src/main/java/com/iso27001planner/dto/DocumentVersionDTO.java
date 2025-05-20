package com.iso27001planner.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVersionDTO {
    private Long id;
    private String version;
    private String status;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String approver;
    private LocalDate approvalDate;
    private LocalDate uploadedAt;
    private String contentPreview; // optional
}
