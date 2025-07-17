package com.iso27001planner.service;

import com.iso27001planner.dto.DocumentDTO;
import com.iso27001planner.dto.DocumentVersionDTO;
import com.iso27001planner.entity.Company;
import com.iso27001planner.entity.Document;
import com.iso27001planner.entity.DocumentVersion;
import com.iso27001planner.event.AuditEvent;
import com.iso27001planner.exception.BusinessException;
import com.iso27001planner.mapper.DocumentMapper;
import com.iso27001planner.repository.CompanyRepository;
import com.iso27001planner.repository.DocumentRepository;
import com.iso27001planner.repository.DocumentVersionRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository versionRepository;
    private final CompanyRepository companyRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final DocumentMapper mapper;
    private final NotificationService notificationService;

    private final Path rootDir = Paths.get("uploads/documents");

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(rootDir);
    }

    public List<DocumentDTO> getAllCurrentDocuments() {
        return documentRepository.findAllActive().stream()
                .map(mapper::toDTO)
                .toList();
    }

//    public List<DocumentDTO> getDocumentsByCompany(Long companyId) {
//        return documentRepository.findByCompanyId(companyId)
//                .stream()
//                .map(mapper::toDTO)
//                .toList();
//    }

    public Page<DocumentDTO> getDocumentsByCompany(
            Long companyId,
            int page,
            int size,
            String search,
            String type,
            String sortBy,
            String sortOrder
    ) {
        Sort sort = sortOrder.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return documentRepository.searchDocuments(companyId, search, type, pageable)
                .map(mapper::toDTO);
    }

    public DocumentDTO getById(Long id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Document not found", HttpStatus.NOT_FOUND));
        return mapper.toDTO(doc);
    }

    @Transactional
    public void delete(Long id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Document not found", HttpStatus.NOT_FOUND));

        doc.setDeleted(true); //  Soft delete
        eventPublisher.publishEvent(new AuditEvent(
                this,
                "Document deleted",
                getCurrentUserEmail(),
                "Document",
                id.toString(),
                "Document deleted : " + id.toString()
        ));
        documentRepository.save(doc);
    }

    @Transactional
    public DocumentDTO createDocument(DocumentDTO dto, MultipartFile file) throws IOException {
        Company company = companyRepository.findById(dto.getCompanyId())
                .orElseThrow(() -> new BusinessException("Company not found", HttpStatus.NOT_FOUND));

        Document doc = Document.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .type(dto.getType())
                .status("draft")
                .version("1.0")
                .owner(dto.getOwner())
                .approver(null)
                .approvalDate(null)
                .reviewDate(LocalDate.parse(dto.getReviewDate()))
                .content(dto.getContent())
                .company(company)
                .build();

        Document saved = documentRepository.save(doc);
        saveVersion(saved, file, "1.0");

        eventPublisher.publishEvent(new AuditEvent(
                this,
                "New draft Document added",
                getCurrentUserEmail(),
                "Document",
                doc.getId().toString(),
                "New employee added : " + doc.getTitle()
        ));

        return toDTO(saved);
    }

    @Transactional
    public void uploadNewVersion(Long docId, MultipartFile file) throws IOException {
        Document doc = documentRepository.findById(docId)
                .orElseThrow(() -> new BusinessException("Document not found", HttpStatus.NOT_FOUND));

        String currentVersion = doc.getVersion();
        String oldStatus = doc.getStatus();

        // Always set status back to review if being updated
        String newStatus = "Review";
        doc.setStatus(newStatus);
        doc.setReviewDate(LocalDate.now().plusMonths(1));

        // Auto-increment version based on status change
        String nextVersion = calculateNextVersion(currentVersion, oldStatus, newStatus);
        doc.setVersion(nextVersion);

        documentRepository.save(doc);

        saveVersion(doc, file, nextVersion);

        eventPublisher.publishEvent(new AuditEvent(
                this,
                "New Document Version uploaded",
                getCurrentUserEmail(),
                "Document",
                doc.getId().toString(),
                "New document version uploaded : " + doc.getTitle()
        ));
    }

    @Transactional
    public void updateDocument(Long docId, DocumentDTO dto, MultipartFile file) throws IOException {
        Document doc = documentRepository.findById(docId)
                .orElseThrow(() -> new BusinessException("Document not found", HttpStatus.NOT_FOUND));



        String currentVersion = doc.getVersion();
        String oldStatus = doc.getStatus();
        String newStatus = dto.getStatus();

        // Update metadata from DTO
        doc.setTitle(dto.getTitle());
        doc.setDescription(dto.getDescription());
        doc.setType(dto.getType());
        doc.setOwner(dto.getOwner());
        doc.setApprover(dto.getApprover());
        doc.setStatus(newStatus);
        if (newStatus == "Review"){
            notificationService.notifyDocumentApproval(doc.getApprover(), doc.getTitle());
        }
        doc.setReviewDate(LocalDate.parse(dto.getReviewDate()));

        // Calculate and apply version bump
        String nextVersion = calculateNextVersion(currentVersion, oldStatus, newStatus);
        doc.setVersion(nextVersion);

        documentRepository.save(doc);

        // 📁 Save file or reuse old file from last version
        if (file != null && !file.isEmpty()) {
            saveVersion(doc, file, nextVersion); // Upload new file
        } else {
            usePreviousFileAsNewVersion(doc, nextVersion); // Reuse last version's file
        }

        eventPublisher.publishEvent(new AuditEvent(
                this,
                "Document updated",
                getCurrentUserEmail(),
                "Document",
                doc.getId().toString(),
                "New document version uploaded : " + doc.getTitle()
        ));
    }

    @Transactional
    public void approveDocument(Long docId, String approverEmail) {
        Document doc = documentRepository.findById(docId)
                .orElseThrow(() -> new BusinessException("Document not found", HttpStatus.NOT_FOUND));

        String currentVersion = doc.getVersion();
        String oldStatus = doc.getStatus();
        String newStatus = "Approved";

        // Update approval-specific fields only
        doc.setStatus(newStatus);
        doc.setApprover(approverEmail);
        doc.setApprovalDate(LocalDate.now());
        doc.setReviewDate(LocalDate.now().plusMonths(1));

        // Calculate version bump
        String nextVersion = calculateNextVersion(currentVersion, oldStatus, newStatus);
        doc.setVersion(nextVersion);

        documentRepository.save(doc);

        // Save the approval action as a new version using the latest file (reuse)
        usePreviousFileAsNewVersion(doc, nextVersion);

        eventPublisher.publishEvent(new AuditEvent(
                this,
                "APPROVE_DOCUMENT",
                approverEmail,
                "Document",
                doc.getId().toString(),
                "Approved document: " + doc.getTitle()
        ));
    }

    private void usePreviousFileAsNewVersion(Document doc, String versionLabel) {
        DocumentVersion last = versionRepository.findTopByDocumentOrderByUploadedAtDesc(doc)
                .orElseThrow(() -> new BusinessException("No previous version to copy", HttpStatus.BAD_REQUEST));

        DocumentVersion newVersion = DocumentVersion.builder()
                .version(versionLabel)
                .status("Review")
                .uploadedAt(LocalDate.now())
                .filePath(last.getFilePath())
                .fileType(last.getFileType())
                .fileName(last.getFileName())
                .fileSize(last.getFileSize())
                .document(doc)
                .build();

        versionRepository.save(newVersion);
    }

    private void saveVersion(Document doc, MultipartFile file, String versionLabel) throws IOException {
        String filename = UUID.randomUUID() + "-" + file.getOriginalFilename();
        Path dest = rootDir.resolve(filename);
        Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

        DocumentVersion version = DocumentVersion.builder()
                .version(versionLabel)
                .status("Review")
                .uploadedAt(LocalDate.now())
                .filePath(dest.toString())
                .fileType(file.getContentType())
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .document(doc)
                .build();

        versionRepository.save(version);
    }

    private DocumentDTO toDTO(Document doc) {
        return new DocumentDTO(
                doc.getId(),
                doc.getTitle(),
                doc.getDescription(),
                doc.getType(),
                doc.getStatus(),
                doc.getVersion(),
                doc.getOwner(),
                doc.getApprover(),
                doc.getApprovalDate() != null ? doc.getApprovalDate().toString() : null,
                doc.getReviewDate() != null ? doc.getReviewDate().toString() : null,
                doc.getContent(),
                doc.getRelatedControls(),
                doc.getCompany().getId()
        );
    }

    private String calculateNextVersion(String current, String oldStatus, String newStatus) {
        if (current == null || current.isBlank()) return "1.0";

        String[] parts = current.split("\\.");

        int major = Integer.parseInt(parts[0]);


        int minor = Integer.parseInt(parts[1]);

        // If transitioning to or from approved status → major bump
        if ("Approved".equalsIgnoreCase(oldStatus) || "Approved".equalsIgnoreCase(newStatus)) {
            return (major + 1) + ".0";
        }

        // Otherwise: minor revision

        return major + "." + (minor + 1);

    }

    @Transactional
    public void updateStatus(Long docId, String newStatus) {
        Document doc = documentRepository.findById(docId)
                .orElseThrow(() -> new BusinessException("Document not found", HttpStatus.NOT_FOUND));

        String oldStatus = doc.getStatus();
        if (!Objects.equals(oldStatus, newStatus)) {
            String nextVersion = calculateNextVersion(doc.getVersion(), oldStatus, newStatus);
            doc.setVersion(nextVersion);
            doc.setStatus(newStatus);

            if ("Approved".equalsIgnoreCase(newStatus)) {
                doc.setApprovalDate(LocalDate.now());
                doc.setApprover(getCurrentUserEmail());
            }

            documentRepository.save(doc);

            // Audit
            eventPublisher.publishEvent(new AuditEvent(
                    this, "UPDATE_DOCUMENT_STATUS", getCurrentUserEmail(), "Document",
                    doc.getId().toString(), "Changed status to " + newStatus + " (v" + nextVersion + ")"
            ));
        }
    }

    public List<DocumentVersionDTO> getVersionHistory(Long documentId) {
        List<DocumentVersion> versions = versionRepository.findByDocument_IdOrderByUploadedAtDesc(documentId);

        return versions.stream()
                .map(v -> new DocumentVersionDTO(
                        v.getId(),
                        v.getVersion(),
                        v.getStatus(),
                        v.getFileName(),
                        v.getFileType(),
                        v.getFileSize(),
                        v.getApprover(),
                        v.getApprovalDate(),
                        v.getUploadedAt(),
                        readPreviewContent(v) // may return null
                ))
                .toList();
    }

    private String readPreviewContent(DocumentVersion version) {
        try {
            Path path = Paths.get(version.getFilePath());
            String mime = version.getFileType();

            if (mime == null) return null;

            if (mime.startsWith("text")) {
                return readPlainText(path);
            } else if (mime.equals("application/pdf")) {
                return readPdfText(path);
            } else if (mime.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
                return readDocxText(path);
            }
        } catch (Exception e) {
            return "[Preview unavailable: " + e.getMessage() + "]";
        }
        return "[Preview not available for this file type or size]";
    }

    private String readPlainText(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8)
                .substring(0, Math.min(1000, (int) Files.size(path))) + "...";
    }

    private String readPdfText(Path path) throws IOException {
        try (PDDocument document = PDDocument.load(path.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            return text.substring(0, Math.min(text.length(), 1000)) + "...";
        }
    }

    private String readDocxText(Path path) throws IOException {
        try (XWPFDocument docx = new XWPFDocument(Files.newInputStream(path))) {
            XWPFWordExtractor extractor = new XWPFWordExtractor(docx);
            String text = extractor.getText();
            return text.substring(0, Math.min(text.length(), 1000)) + "...";
        }
    }

    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";
    }
}
