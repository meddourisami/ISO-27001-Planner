package com.iso27001planner.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iso27001planner.dto.BackupDataDTO;
import com.iso27001planner.entity.Company;
import com.iso27001planner.entity.Document;
import com.iso27001planner.entity.DocumentVersion;
import com.iso27001planner.event.AuditEvent;
import com.iso27001planner.exception.BusinessException;
import com.iso27001planner.repository.*;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BackupService {

    private final CompanyRepository companyRepo;
    private final UserRepository userRepo;
    private final DocumentRepository documentRepo;
    private final RiskRepository riskRepo;
    private final ControlRepository controlRepo;
    private final ApplicationEventPublisher eventPublisher;

    private static final String ENCRYPTION_KEY = "wl3nPe37_duSvEJSY84AgE8VxPF_Qhne";
    private static final String AES = "AES";
    private final AssetRepository assetRepository;
    private final ObjectMapper mapper = new ObjectMapper();
    private final NonConformityRepository nonConformityRepository;
    private final TaskRepository taskRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final AuditPlanRepository auditPlanRepository;
    private final TrainingRepository trainingRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationRepository notificationRepository;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), AES);
    }

    public File exportEncryptedBackup(Long companyId) throws Exception {
        Company company = companyRepo.findById(companyId)
                .orElseThrow(() -> new BusinessException("Company not found", HttpStatus.NOT_FOUND));

        BackupDataDTO backup = new BackupDataDTO();
        backup.setCompany(company);
        backup.setUsers(userRepo.findByCompany(company));
        backup.setControls((controlRepo.findByCompany_Id(companyId)));
        backup.setAssets(assetRepository.findByCompany_Id(companyId));
        backup.setRisks(riskRepo.findByCompany_Id(companyId));
        backup.setNonConformities((nonConformityRepository.findByCompany_Id(companyId)));
        backup.setTasks(taskRepository.findByCompany_Id(companyId));
        // 1. Fetch documents by company
        List<Document> documents = documentRepo.findByCompany_Id(companyId);
        backup.setDocuments(documents);

        // 2. Collect document IDs
        List<Long> documentIds = documents.stream()
                .map(Document::getId)
                .collect(Collectors.toList());

        // 3. Fetch related versions
        List<DocumentVersion> versions = documentVersionRepository.findByDocument_IdIn(documentIds);
        backup.setDocumentVersions(versions);
        backup.setAuditPlans(auditPlanRepository.findByCompany_Id(companyId));
        backup.setTrainings(trainingRepository.findByCompany_Id(companyId));
        backup.setEmployees(employeeRepository.findByCompany_Id(companyId));
        backup.setNotifications((notificationRepository.findByCompanyId(companyId)));

        byte[] jsonBytes = mapper.writeValueAsBytes(backup);

        String sanitized = company.getName().toLowerCase().replaceAll("[^a-z0-9]", "-");
        String fileName = String.format("backup-%s-%s.enc", sanitized, LocalDate.now());
        Path backupDir = Paths.get("backups"); // make sure this exists
        Files.createDirectories(backupDir);    // safe to call multiple times
        File outFile = new File(backupDir.toFile(), fileName);
        try (FileOutputStream fos = new FileOutputStream(outFile);
             CipherOutputStream cos = new CipherOutputStream(fos, getCipher(Cipher.ENCRYPT_MODE))) {
            cos.write(jsonBytes);
        }

        eventPublisher.publishEvent(new AuditEvent(this, "BACKUP_CREATED", getCurrentUserEmail(),
                "Backup", companyId.toString(), "Encrypted backup created"));
        return outFile;
    }

    @Transactional
    public void restoreEncryptedBackup(File encryptedFile) throws Exception {
        // 🔓 Decrypt backup file
        byte[] decryptedBytes;
        try (FileInputStream fis = new FileInputStream(encryptedFile);
             CipherInputStream cis = new CipherInputStream(fis, getCipher(Cipher.DECRYPT_MODE))) {
            decryptedBytes = cis.readAllBytes();
        }

        // 🔄 Deserialize backup data
        BackupDataDTO backup = mapper.readValue(decryptedBytes, BackupDataDTO.class);
        Company company = backup.getCompany();

        // Re-save company first
        Company savedCompany = companyRepo.save(company);

        // Users
        backup.getUsers().forEach(user -> user.setCompany(savedCompany));
        userRepo.saveAll(backup.getUsers());

        // Controls
        backup.getControls().forEach(control -> control.setCompany(savedCompany));
        controlRepo.saveAll(backup.getControls());

        // Assets
        backup.getAssets().forEach(asset -> asset.setCompany(savedCompany));
        assetRepository.saveAll(backup.getAssets());

        // Risks
        backup.getRisks().forEach(risk -> risk.setCompany(savedCompany));
        riskRepo.saveAll(backup.getRisks());

        // Non-conformities
        backup.getNonConformities().forEach(nc -> nc.setCompany(savedCompany));
        nonConformityRepository.saveAll(backup.getNonConformities());

        // Tasks
        backup.getTasks().forEach(task -> task.setCompany(savedCompany));
        taskRepository.saveAll(backup.getTasks());

        // Documents
        backup.getDocuments().forEach(doc -> doc.setCompany(savedCompany));
        documentRepo.saveAll(backup.getDocuments());

        // Document Versions (bind to existing documents)
        documentVersionRepository.saveAll(backup.getDocumentVersions());

        // Audit Plans
        backup.getAuditPlans().forEach(audit -> audit.setCompany(savedCompany));
        auditPlanRepository.saveAll(backup.getAuditPlans());

        // Trainings
        backup.getTrainings().forEach(training -> training.setCompany(savedCompany));
        trainingRepository.saveAll(backup.getTrainings());

        // Employees
        backup.getEmployees().forEach(emp -> emp.setCompany(savedCompany));
        employeeRepository.saveAll(backup.getEmployees());

        // Notifications
        backup.getNotifications().forEach(note -> note.setCompany(savedCompany));
        notificationRepository.saveAll(backup.getNotifications());

        eventPublisher.publishEvent(new AuditEvent(
                this,
                "BACKUP_RESTORED",
                getCurrentUserEmail(),
                "Backup",
                company.getId().toString(),
                "Restored encrypted backup for company: " + company.getName()
        ));
    }

    private Cipher getCipher(int mode) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(AES);
        cipher.init(mode, secretKey);
        return cipher;
    }

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
