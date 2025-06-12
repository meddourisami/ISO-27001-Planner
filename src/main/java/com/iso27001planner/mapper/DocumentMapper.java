package com.iso27001planner.mapper;

import com.iso27001planner.dto.DocumentDTO;
import com.iso27001planner.entity.Document;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DocumentMapper {

    public DocumentDTO toDTO(Document doc) {
        return DocumentDTO.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .description(doc.getDescription())
                .type(doc.getType())
                .status(doc.getStatus())
                .version(doc.getVersion())
                .owner(doc.getOwner())
                .approver(doc.getApprover())
                .approvalDate(doc.getApprovalDate() != null ? doc.getApprovalDate().toString() : null)
                .reviewDate(doc.getReviewDate() != null ? doc.getReviewDate().toString() : null)
                .content(doc.getContent()) // if needed
                .build();
    }

    public Document toEntity(DocumentDTO dto) {
        return Document.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .type(dto.getType())
                .status(dto.getStatus())
                .version(dto.getVersion())
                .owner(dto.getOwner())
                .approver(dto.getApprover())
                .approvalDate(dto.getApprovalDate() != null ? LocalDate.parse(dto.getApprovalDate()) : null)
                .reviewDate(dto.getReviewDate() != null ? LocalDate.parse(dto.getReviewDate()) : null)
                .content(dto.getContent())
                .build();
    }
}