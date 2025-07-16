package com.iso27001planner.controller;

import com.iso27001planner.entity.AuditLog;
import com.iso27001planner.entity.User;
import com.iso27001planner.exception.BusinessException;
import com.iso27001planner.repository.AuditEventRepository;
import com.iso27001planner.repository.UserRepository;
import com.iso27001planner.service.AuditExportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditEventRepository repository;
    private final AuditExportService auditExportService;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ISMS_ADMIN')")
    public ResponseEntity<?> getAll(
            @RequestParam Optional<String> user,
            @RequestParam Optional<String> action,
            @RequestParam Optional<String> entity,
            @RequestParam Optional<LocalDate> from,
            @RequestParam Optional<LocalDate> to
    ) {
        try {
            String requesterEmail = SecurityContextHolder.getContext().getAuthentication().getName();

            User requester = userRepository.findByEmail(requesterEmail)
                    .orElseThrow(() -> new BusinessException("User not found", HttpStatus.UNAUTHORIZED));

            String requesterRole = requester.getRole().name();
            boolean isSuperAdmin = "SUPER_ADMIN".equals(requesterRole);

            Long companyId = requester.getCompany().getId();

            List<AuditLog> logs = repository.findTop100ByOrderByTimestampDesc();

            List<AuditLog> filtered = logs.stream()
                    .filter(log -> {
                        try {
                            return isSuperAdmin || isLogRelatedToCompany(log, companyId);
                        } catch (Exception e) {
                            System.err.println("❌ Failed to check company for log " + log.getId() + ": " + e.getMessage());
                            return false;
                        }
                    })
                    .filter(log -> user.map(u -> log.getActorEmail().equalsIgnoreCase(u)).orElse(true))
                    .filter(log -> action.map(a -> log.getActionType().equalsIgnoreCase(a)).orElse(true))
                    .filter(log -> entity.map(e -> log.getEntityType().equalsIgnoreCase(e)).orElse(true))
                    .filter(log -> from.map(start -> !log.getTimestamp().toLocalDate().isBefore(start)).orElse(true))
                    .filter(log -> to.map(end -> !log.getTimestamp().toLocalDate().isAfter(end)).orElse(true))
                    .sorted(Comparator.comparing(AuditLog::getTimestamp).reversed())
                    .toList();

            return ResponseEntity.ok(filtered);
        } catch (DateTimeParseException ex) {
            return ResponseEntity.badRequest().body("⛔ Invalid date format. Expected format: yyyy-MM-dd");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("⚠️ Error fetching audit logs: " + ex.getMessage());
        }
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public void exportCsv(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=audit-log.csv");
        auditExportService.exportCsv(response.getOutputStream());
    }

    @GetMapping("/export/pdf")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public void exportPdf(HttpServletResponse response) throws Exception {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=audit-log.pdf");
        auditExportService.exportPdf(response.getOutputStream());
    }

//    @GetMapping("/export/pdf-table")
//    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
//    public void exportPdfTable(HttpServletResponse response) throws Exception {
//        response.setContentType("application/pdf");
//        response.setHeader("Content-Disposition", "attachment; filename=audit-log-table.pdf");
//        auditExportService.exportPdfTable(response.getOutputStream());
//    }

    @GetMapping("/export/audit/pdf-table/{companyId}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public void exportAuditPdf(
            @PathVariable Long companyId,
            @RequestParam List<String> sections,
            HttpServletResponse response
    ) throws Exception {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=audit-report.pdf");
        auditExportService.writeAuditPdfWithSections(sections, response.getOutputStream());
    }

    private boolean isLogRelatedToCompany(AuditLog log, Long companyId) {
        // If the log's actor belongs to the company, include it.
        return userRepository.findByEmail(log.getActorEmail())
                .map(user -> user.getCompany().getId().equals(companyId))
                .orElse(false);
    }
}
