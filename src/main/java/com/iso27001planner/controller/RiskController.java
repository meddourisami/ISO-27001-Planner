package com.iso27001planner.controller;

import com.iso27001planner.dto.RiskDTO;
import com.iso27001planner.service.CsvExportService;
import com.iso27001planner.service.PdfExportService;
import com.iso27001planner.service.RiskService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/risks")
@RequiredArgsConstructor
public class RiskController {

    private final RiskService riskService;
    private final CsvExportService csvExportService;
    private final PdfExportService pdfExportService;

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RiskDTO> getRiskById(@PathVariable String id) {
        return ResponseEntity.ok(riskService.getRiskById(id));
    }

    @PreAuthorize("hasAuthority('ISMS_ADMIN')")
    @PostMapping
    public ResponseEntity<RiskDTO> create(@RequestBody RiskDTO dto) {
        return ResponseEntity.ok(riskService.createRisk(dto));
    }

    @PreAuthorize("hasAuthority('ISMS_ADMIN')")
    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<RiskDTO>> listByCompany(@PathVariable Long companyId) {
        return ResponseEntity.ok(riskService.listCompanyRisks(companyId));
    }

    @PreAuthorize("hasAuthority('ISMS_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<RiskDTO> update(@PathVariable String id, @RequestBody RiskDTO dto) {
        return ResponseEntity.ok(riskService.updateRisk(id, dto));
    }

    @PreAuthorize("hasAuthority('ISMS_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        riskService.deleteRisk(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('ISMS_ADMIN')")
    @GetMapping("/matrix/{companyId}")
    public ResponseEntity<Map<String, Map<String, Long>>> getMatrix(@PathVariable Long companyId) {
        return ResponseEntity.ok(riskService.getRiskMatrix(companyId));
    }

    @GetMapping("/export/csv/{companyId}")
    @PreAuthorize("hasAuthority('ISMS_ADMIN')")
    public void exportCsv(@PathVariable Long companyId, HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=risk-report.csv");
        csvExportService.writeRisksToCsv(companyId, response.getOutputStream());
    }

    @GetMapping("/export/pdf/{companyId}")
    @PreAuthorize("hasAuthority('ISMS_ADMIN')")
    public void exportPdf(@PathVariable Long companyId, HttpServletResponse response) throws Exception {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=risk-report.pdf");
        pdfExportService.writeRisksToPdf(companyId, response.getOutputStream());
    }

    @GetMapping("/export/pdf-table/{companyId}")
    @PreAuthorize("hasAuthority('ISMS_ADMIN')")
    public void exportPdfTable(@PathVariable Long companyId, HttpServletResponse response) throws Exception {
        response.setContentType("application/pdf");
        String filename = "risk-report_" + LocalDate.now() + ".pdf";
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);
        pdfExportService.writeRisksToPdfTable(companyId, response.getOutputStream());
    }
}
