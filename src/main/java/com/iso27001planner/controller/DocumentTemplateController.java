package com.iso27001planner.controller;

import com.iso27001planner.entity.DocumentTemplate;
import com.iso27001planner.service.DocumentTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class DocumentTemplateController {

    private final DocumentTemplateService templateService;

    @PostMapping("/upload")
    public ResponseEntity<DocumentTemplate> uploadTemplate(
            @RequestParam("file") MultipartFile file,
            @RequestParam String title,
            @RequestParam String type,
            @RequestParam String isoClause,
            @RequestParam String description
    ) throws IOException {
        DocumentTemplate template = templateService.uploadTemplate(file, title, type, isoClause, description);
        return ResponseEntity.status(HttpStatus.CREATED).body(template);
    }

    @GetMapping
    public ResponseEntity<List<DocumentTemplate>> listTemplates() {
        return ResponseEntity.ok(templateService.listGlobalTemplates());
    }

    @GetMapping("/search")
    public ResponseEntity<List<DocumentTemplate>> search(@RequestParam String q) {
        return ResponseEntity.ok(templateService.searchByKeyword(q));
    }
}
