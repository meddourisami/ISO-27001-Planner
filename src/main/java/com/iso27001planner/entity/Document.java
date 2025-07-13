package com.iso27001planner.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    private String type;    // policy, procedure, record, standard
    private String status;  // draft, under_review, approved

    private String version;

    private String owner;
    private String approver;

    private LocalDate approvalDate;
    private LocalDate reviewDate;

    @Column(nullable = false)
    private boolean deleted = false;

    private String content; // Text content (optional) if stored in DB
    private String relatedControls;

    private String fileName; // Original name
    private String filePath; // Stored path or key
    private String fileType; // mime-type
    private Long fileSize;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DocumentVersion> versions = new ArrayList<>();
}
