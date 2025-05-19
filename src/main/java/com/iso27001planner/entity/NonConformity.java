package com.iso27001planner.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NonConformity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String title;
    private String description;

    private String source;             // e.g. "Internal Audit", "Incident", "Observation"
    private String sourceReference;    // e.g. AuditPlan ID, Incident ID
    private LocalDate dateIdentified;

    private String severity;           // e.g. Low, Medium, High, Critical
    private String status;             // e.g. Open, In Progress, Resolved, Verified
    private String owner;

    private LocalDate dueDate;

    @ElementCollection
    private List<String> relatedControls;

    @ElementCollection
    private List<String> relatedRisks;

    private String correctiveActions;
    private String evidence;
    private String evidencePath; // store file path, not binary

    private String verificationStatus;  // e.g. Pending, Verified, Rejected
    private LocalDate verificationDate;
    private String verifiedBy;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;
}
