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

    @PostMapping
    @PreAuthorize("hasAuthority('ISMS_ADMIN')")
    public ResponseEntity<DocumentDTO> create(
            @RequestPart("data") DocumentDTO dto,
            @RequestPart("file") MultipartFile file
    ) throws IOException {
        return ResponseEntity.ok(documentService.createDocument(dto, file));
    }

    @PutMapping("/{id}/upload")
    @PreAuthorize("hasAuthority('ISMS_ADMIN')")
    public ResponseEntity<String> uploadVersion(
            @PathVariable Long id,
            @RequestParam("version") String version,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        documentService.uploadNewVersion(id, file);
        return ResponseEntity.ok("New version uploaded.");
    }

    @GetMapping("/{id}/versions")
    @PreAuthorize("hasAnyAuthority('ISMS_ADMIN', 'COMPANY_USER')")
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
}
