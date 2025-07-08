package com.iso27001planner.controller;

import com.iso27001planner.dto.DocumentDTO;
import com.iso27001planner.dto.DocumentVersionDTO;
import com.iso27001planner.entity.DocumentVersion;
import com.iso27001planner.exception.BusinessException;
import com.iso27001planner.mapper.DocumentVersionMapper;
import com.iso27001planner.repository.DocumentVersionRepository;
import com.iso27001planner.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentVersionRepository versionRepository;
    private final DocumentVersionMapper mapper;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ISMS_ADMIN', 'ISMS_USER')")
    public ResponseEntity<List<DocumentDTO>> getAllDocuments() {
        return ResponseEntity.ok(documentService.getAllCurrentDocuments());
    }

    @GetMapping("/company/{companyId}")
    @PreAuthorize("hasAnyAuthority('ISMS_ADMIN', 'ISMS_USER')")
    public ResponseEntity<List<DocumentDTO>> getCompanyDocuments(@PathVariable Long companyId) {
        return ResponseEntity.ok(documentService.getDocumentsByCompany(companyId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ISMS_ADMIN', 'ISMS_USER')")
    public ResponseEntity<DocumentDTO> getDocumentById(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.getById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ISMS_ADMIN')")
    public ResponseEntity<String> deleteDocument(@PathVariable Long id) {
        documentService.delete(id);
        return ResponseEntity.ok("Document deleted successfully.");
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ISMS_ADMIN')")
    public ResponseEntity<DocumentDTO> create(
            @RequestPart("data") DocumentDTO dto,
            @RequestPart("file") MultipartFile file
    ) throws IOException {
        return ResponseEntity.ok(documentService.createDocument(dto, file));
    }

    @PutMapping("/{id}/update")
    @PreAuthorize("hasAuthority('ISMS_ADMIN')")
    public ResponseEntity<String> updateDocument(
            @PathVariable Long id,
            @RequestPart("data") DocumentDTO dto,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) throws IOException {
        documentService.updateDocument(id, dto, file);
        return ResponseEntity.ok("Document updated successfully.");
    }

    @GetMapping("/{id}/versions")
    @PreAuthorize("hasAnyAuthority('ISMS_ADMIN', 'ISMS_USER')")
    public ResponseEntity<List<DocumentVersionDTO>> getVersions(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.getVersionHistory(id));
    }

    @GetMapping("/download/{versionId}")
    public ResponseEntity<Resource> download(@PathVariable Long versionId) throws IOException {
        DocumentVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new BusinessException("Version not found", HttpStatus.NOT_FOUND));

        Path path = Paths.get(version.getFilePath());
        Resource resource = new UrlResource(path.toUri());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(version.getFileType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + version.getFileName() + "\"")
                .body(resource);
    }

    @GetMapping("/search")
    public ResponseEntity<List<DocumentVersionDTO>> search(@RequestParam String query) {
        List<DocumentVersion> results = versionRepository.searchByContent(query);
        return ResponseEntity.ok(results.stream().map(mapper::toDTO).toList());
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('ISMS_ADMIN')")
    public ResponseEntity<String> approveDocument(@PathVariable Long id) {
        String approverEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        documentService.approveDocument(id, approverEmail);
        return ResponseEntity.ok("Document approved successfully.");
    }

}
