package com.iso27001planner.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String name;
    private String description;
    private String type;
    private String classification;
    private String location;
    private String status;
    private String value;
    private String vulnerabilities;
    private String controls;
    private String lastReview;

    @ElementCollection
    private List<String> relatedRisks;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;
}
