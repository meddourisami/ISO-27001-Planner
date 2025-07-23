package com.iso27001planner.controller;

import com.iso27001planner.service.ComplianceReportService;
import com.iso27001planner.service.ExecutiveReportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports/")
@RequiredArgsConstructor
public class ReportController {
    private final ExecutiveReportService executiveReportService;
    private final ComplianceReportService complianceReportService;

    @GetMapping("/executive-report/{companyId}")
    @PreAuthorize("hasAuthority('ISMS_ADMIN')")
    public void generateExecutiveReport(
            @PathVariable Long companyId,
            @RequestParam List<String> sections,
            HttpServletResponse response
    ) throws Exception {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=executive-summary.pdf");
        executiveReportService.exportExecutiveSummary(companyId, sections, response.getOutputStream());
    }

    @GetMapping("/compliance-report/{companyId}")
    @PreAuthorize("hasAuthority('ISMS_ADMIN')")
    public void downloadComplianceReport(
            @PathVariable Long companyId,
            @RequestParam List<String> sections,
            HttpServletResponse response
    ) throws Exception {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=compliance-report.pdf");
        complianceReportService.exportComplianceReport(companyId, sections, response.getOutputStream());
    }
}
