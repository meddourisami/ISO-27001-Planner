package com.iso27001planner.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NonConformityDTO {
    private String id;
    private String title;
    private String description;
    private String source;
    private String sourceReference;
    private String dateIdentified;
    private String severity;
    private String status;
    private String owner;
    private String dueDate;
    private List<String> relatedControls; // control UUIDs
    private List<String> relatedRisks;    // risk UUIDs
    private String correctiveActions;
    private String evidence;
    private String verificationStatus;
    private String verificationDate;
    private String verifiedBy;
    private String comments;
    private Long companyId;
}
