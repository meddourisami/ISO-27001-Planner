package com.iso27001planner.service;

import com.iso27001planner.entity.DocumentTemplate;
import com.iso27001planner.repository.DocumentTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentTemplateService {

    private final DocumentTemplateRepository templateRepository;

    private final Path rootDir = Paths.get("uploads/templates");

    public DocumentTemplate uploadTemplate(MultipartFile file, String title, String type, String isoClause, String description) throws IOException {
        Files.createDirectories(rootDir);

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path targetPath = rootDir.resolve(fileName);

        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        DocumentTemplate template = DocumentTemplate.builder()
                .title(title)
                .type(type)
                .isoClause(isoClause)
                .description(description)
                .fileName(file.getOriginalFilename())
                .filePath(targetPath.toString())
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .build();

        return templateRepository.save(template);
    }

    public List<DocumentTemplate> listGlobalTemplates() {
        return templateRepository.findByCompanyIsNull();
    }

    public List<DocumentTemplate> searchByKeyword(String keyword) {
        return templateRepository.searchByContent(keyword);
    }
}
