package com.iso27001planner.controller;

import com.iso27001planner.dto.CompanyDTO;
import com.iso27001planner.dto.UpdateCompanyRequest;
import com.iso27001planner.entity.Company;
import com.iso27001planner.exception.BusinessException;
import com.iso27001planner.repository.CompanyRepository;
import com.iso27001planner.service.BackupService;
import com.iso27001planner.service.CompanyService;
import com.iso27001planner.service.DocumentTemplateService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final DocumentTemplateService templateService;
    private final BackupService backupService;
    private final CompanyRepository companyRepo;

    @PutMapping("/update")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ISMS_ADMIN')")
    public ResponseEntity<String> updateCompany(@RequestBody UpdateCompanyRequest request) {
        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        if (request.getName().isBlank() || request.getIsmsScope().isBlank()) {
            throw new BusinessException("Company name and ISMS scope must not be blank", HttpStatus.BAD_REQUEST);
        }
        companyService.updateCompany(request, actor);
        return ResponseEntity.ok("Company updated successfully.");
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CompanyDTO> getMyCompany() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(companyService.getCurrentUserCompany(email));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ISMS_ADMIN')")
    public ResponseEntity<CompanyDTO> getCompanyById(@PathVariable Long id) {
        return ResponseEntity.ok(companyService.getCompanyById(id));
    }

    @PostMapping("/seed-templates/{companyId}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<String> seedTemplates(@PathVariable Long companyId) throws IOException {
        templateService.seedTemplatesToCompany(companyId);
        return ResponseEntity.ok("Templates seeded to company.");
    }

    @PostMapping("/backup")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ISMS_ADMIN')")
    public void backupCompany(@RequestParam Long companyId, HttpServletResponse response) throws Exception {
        File file = backupService.exportEncryptedBackup(companyId);

        // Send it to client
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=" + file.getName());
        Files.copy(file.toPath(), response.getOutputStream());
    }

    @PostMapping("/restore")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ISMS_ADMIN')")
    public ResponseEntity<String> restoreCompany(@RequestParam Long companyId) {
        try {
            Company company = companyRepo.findById(companyId)
                    .orElseThrow(() -> new BusinessException("Company not found", HttpStatus.NOT_FOUND));

            String sanitized = company.getName().toLowerCase().replaceAll("[^a-z0-9]", "-");
            String prefix = String.format("backup-%s-", sanitized);
            String suffix = ".enc";

            Path backupDir = Paths.get("/backups"); // Change to your actual backups path

            if (!Files.exists(backupDir) || !Files.isDirectory(backupDir)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("⛔ Backup directory not found: " + backupDir.toAbsolutePath());
            }

            File latestFile = null;
            LocalDate latestDate = null;

            // Match: backup-<sanitized>-YYYY-MM-DD.enc
            Pattern pattern = Pattern.compile("^" + Pattern.quote(prefix) + "(\\d{4}-\\d{2}-\\d{2})" + Pattern.quote(suffix) + "$");

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(backupDir)) {
                for (Path entry : stream) {
                    String filename = entry.getFileName().toString();
                    Matcher matcher = pattern.matcher(filename);

                    if (matcher.matches()) {
                        LocalDate date = LocalDate.parse(matcher.group(1));
                        if (latestDate == null || date.isAfter(latestDate)) {
                            latestDate = date;
                            latestFile = entry.toFile();
                        }
                    }
                }
            }

            if (latestFile == null || !latestFile.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("⛔ No backup file found for company: " + company.getName());
            }

            // Restore from the most recent file
            backupService.restoreEncryptedBackup(latestFile);

            return ResponseEntity.ok("✅ Company restore completed successfully from: " + latestFile.getName());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("⛔ Restore failed: " + e.getMessage());
        }
    }
}
