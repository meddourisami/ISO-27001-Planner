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
public class DocumentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String version;
    private String status;
    private String approver;
    private LocalDate approvalDate;
    private LocalDate uploadedAt;

    private String filePath;
    private String fileType;
    private String fileName;
    private Long fileSize;

    @Column(columnDefinition = "TEXT")
    private String previewContent; // Indexed and searchable

    @ManyToOne
    @JoinColumn(name = "document_id")
    private Document document;
}
