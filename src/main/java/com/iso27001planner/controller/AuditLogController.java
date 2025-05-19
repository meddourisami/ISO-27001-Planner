package com.iso27001planner.controller;

import com.iso27001planner.entity.AuditLog;
import com.iso27001planner.repository.AuditEventRepository;
import com.iso27001planner.service.AuditExportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditEventRepository repository;
    private final AuditExportService auditExportService;

    @GetMapping
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<List<AuditLog>> getAll(
            @RequestParam Optional<String> user,
            @RequestParam Optional<String> action,
            @RequestParam Optional<String> entity,
            @RequestParam Optional<LocalDate> from,
            @RequestParam Optional<LocalDate> to
    ) {
        List<AuditLog> logs = repository.findAll();

        return ResponseEntity.ok(
                logs.stream()
                        .filter(log -> user.map(u -> log.getActorEmail().equalsIgnoreCase(u)).orElse(true))
                        .filter(log -> action.map(a -> log.getActionType().equalsIgnoreCase(a)).orElse(true))
                        .filter(log -> entity.map(e -> log.getEntityType().equalsIgnoreCase(e)).orElse(true))
                        .filter(log -> from.map(start -> !log.getTimestamp().toLocalDate().isBefore(start)).orElse(true))
                        .filter(log -> to.map(end -> !log.getTimestamp().toLocalDate().isAfter(end)).orElse(true))
                        .sorted(Comparator.comparing(AuditLog::getTimestamp).reversed())
                        .toList()
        );
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

    @GetMapping("/export/pdf-table")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public void exportPdfTable(HttpServletResponse response) throws Exception {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=audit-log-table.pdf");
        auditExportService.exportPdfTable(response.getOutputStream());
    }
}
