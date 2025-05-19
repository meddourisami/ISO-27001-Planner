package com.iso27001planner.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String title;
    private String description;

    private String assignee; // Email or user ID

    private String status;   // open, in_progress, completed, overdue
    private String priority; // low, medium, high, critical

    private LocalDate dueDate;
    private String category;

    private int progress;

    private String relatedControl;

    @ManyToOne
    @JoinColumn(name = "risk_id")
    private Risk risk;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;
}
