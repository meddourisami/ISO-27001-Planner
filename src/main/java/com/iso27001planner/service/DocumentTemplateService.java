package com.iso27001planner.service;

import com.iso27001planner.entity.Company;
import com.iso27001planner.entity.Document;
import com.iso27001planner.entity.DocumentTemplate;
import com.iso27001planner.entity.DocumentVersion;
import com.iso27001planner.exception.BusinessException;
import com.iso27001planner.repository.CompanyRepository;
import com.iso27001planner.repository.DocumentRepository;
import com.iso27001planner.repository.DocumentTemplateRepository;
import com.iso27001planner.repository.DocumentVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentTemplateService {

    private final DocumentTemplateRepository templateRepository;
    private final CompanyRepository companyRepo;
    private final DocumentRepository documentRepo;
    private final DocumentVersionRepository versionRepository;

    private final Path rootDir = Paths.get("uploads/templates");
    private final Path documentDir = Paths.get("uploads/documents");

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

    public void seedTemplatesToCompany(Long companyId) throws IOException {
        Company company = companyRepo.findById(companyId)
                .orElseThrow(() -> new BusinessException("Company not found", HttpStatus.NOT_FOUND));

        Files.createDirectories(documentDir);

        List<DocumentTemplate> globalTemplates = templateRepository.findByCompanyIsNull();

        for (DocumentTemplate template : globalTemplates) {
            Document doc = new Document();
            doc.setTitle(template.getTitle());
            doc.setDescription(template.getDescription());
            doc.setType(template.getType());
            doc.setStatus("draft");
            doc.setVersion("1.0");
            doc.setOwner("system");
            doc.setApprover(null);
            doc.setApprovalDate(null);
            doc.setReviewDate(LocalDate.now().plusMonths(1));
            doc.setFileName(template.getFileName());
            doc.setFilePath(template.getFilePath());
            doc.setFileType(template.getFileType());
            doc.setFileSize(template.getFileSize());
            doc.setCompany(company);
            doc.setRelatedControls(template.getIsoClause());

            Document saved = documentRepo.save(doc);

            saveVersionFromTemplate(saved, template, documentDir);
        }

        System.out.println("✅ Seeded " + globalTemplates.size() + " templates to company " + company.getName());
    }

    private void saveVersionFromTemplate(Document doc, DocumentTemplate template, Path documentDir) throws IOException {
        Path originalPath = Paths.get(template.getFilePath());
        String filename = template.getFileName();
        Path dest = documentDir.resolve(filename);
        Files.copy(originalPath, dest, StandardCopyOption.REPLACE_EXISTING);

        DocumentVersion version = DocumentVersion.builder()
                .version("1.0")
                .status("Draft")
                .uploadedAt(LocalDate.now())
                .filePath(dest.toString())
                .fileType(template.getFileType())
                .fileName(template.getFileName())
                .fileSize(template.getFileSize())
                .document(doc)
                .build();

        versionRepository.save(version);
    }

    public List<DocumentTemplate> listGlobalTemplates() {
        return templateRepository.findByCompanyIsNull();
    }

    public List<DocumentTemplate> searchByKeyword(String keyword) {
        return templateRepository.searchByContent(keyword);
    }
}
