package com.iso27001planner.controller;

import com.iso27001planner.dto.NonConformityDTO;
import com.iso27001planner.entity.NonConformity;
import com.iso27001planner.exception.BusinessException;
import com.iso27001planner.repository.NonConformityRepository;
import com.iso27001planner.service.NonConformityService;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/nonconformities")
@RequiredArgsConstructor
public class NonConformityController {

    private final NonConformityService service;
    private final NonConformityRepository repository;

    @PostMapping
    public ResponseEntity<NonConformityDTO> create(@RequestBody NonConformityDTO dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<NonConformityDTO>> listByCompany(@PathVariable Long companyId) {
        return ResponseEntity.ok(service.listByCompany(companyId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ISMS_ADMIN')")
    public ResponseEntity<NonConformityDTO> updateNonConformity(
            @PathVariable String id,
            @RequestBody NonConformityDTO dto
    ) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ISMS_ADMIN')")
    public ResponseEntity<Void> deleteNonConformity(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/upload-evidence")
    public ResponseEntity<String> uploadEvidence(@PathVariable UUID id,
                                                 @RequestParam MultipartFile file) throws IOException {
        NonConformity nc = repository.findById(id)
                .orElseThrow(() -> new BusinessException("NC not found", HttpStatus.NOT_FOUND));

        Path path = Paths.get("files/evidence/" + id + "_" + file.getOriginalFilename());
        Files.createDirectories(path.getParent());
        Files.write(path, file.getBytes());

        nc.setEvidencePath(path.toString());
        repository.save(nc);

        return ResponseEntity.ok("Uploaded successfully");
    }

    @GetMapping("/{id}/download-evidence")
    public ResponseEntity<Resource> download(@PathVariable UUID id) throws IOException {
        NonConformity nc = repository.findById(id)
                .orElseThrow(() -> new BusinessException("NC not found", HttpStatus.NOT_FOUND));

        Path path = Paths.get(nc.getEvidencePath());
        Resource resource = new UrlResource(path.toUri());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + path.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
