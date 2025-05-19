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
public class Training {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String title;
    private String description;
    private String type; // e.g. security, onboarding
    private String status; // planned, ongoing, completed

    private LocalDate startDate;
    private LocalDate endDate;
    private int duration; // in hours

    private String instructor;
    private String materials;

    @ElementCollection
    private List<String> requiredFor; // e.g. ["IT", "HR"]

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

}
