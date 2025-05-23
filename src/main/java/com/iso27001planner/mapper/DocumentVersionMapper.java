package com.iso27001planner.mapper;

import com.iso27001planner.dto.DocumentVersionDTO;
import com.iso27001planner.entity.DocumentVersion;
import org.springframework.stereotype.Component;

@Component
public class DocumentVersionMapper {
    public DocumentVersionDTO toDTO(DocumentVersion version) {
        DocumentVersionDTO dto = new DocumentVersionDTO();
        dto.setId(version.getId());
        dto.setVersion(version.getVersion());
        dto.setStatus(version.getStatus());
        dto.setFileName(version.getFileName());
        dto.setFileType(version.getFileType());
        dto.setFileSize(version.getFileSize());
        dto.setApprover(version.getApprover());
        dto.setApprovalDate(version.getApprovalDate());
        dto.setUploadedAt(version.getUploadedAt());
        dto.setContentPreview(version.getPreviewContent()); // previewContent → contentPreview
        return dto;
    }
}
