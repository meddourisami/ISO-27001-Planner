package com.iso27001planner.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    private String type; // internal, external
    private String scope;

    private LocalDate startDate;
    private LocalDate endDate;

    private String status; // planned, in_progress, completed
    private String auditor;

    private String findings;
    private int nonConformities;
    private int observations;
    private int recommendations;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;
}
