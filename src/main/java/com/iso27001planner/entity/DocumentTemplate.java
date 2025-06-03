package com.iso27001planner.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;           // e.g. Access Control Policy
    private String type;            // policy, procedure, standard
    private String isoClause;       // e.g. A.5.1.1
    private String description;     // Why this doc is needed

    private String fileName;        // original filename
    private String filePath;        // server/local disk path
    private String fileType;        // MIME type e.g. application/pdf
    private Long fileSize;          // in bytes

    @Column(length = 64, unique = true)
    private String fileHash;

    @Column(length = 10000)
    private String content;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company; // null = global template
}
