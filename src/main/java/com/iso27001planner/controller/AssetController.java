package com.iso27001planner.controller;

import com.iso27001planner.dto.AssetDTO;
import com.iso27001planner.service.AssetExportService;
import com.iso27001planner.service.AssetService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;
    private final AssetExportService assetExportService;

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AssetDTO> getAssetById(@PathVariable String id) {
        return ResponseEntity.ok(assetService.getAssetById(id));
    }

    @PreAuthorize("hasAuthority('ISMS_ADMIN')")
    @PostMapping
    public ResponseEntity<AssetDTO> create(@RequestBody AssetDTO dto) {
        return ResponseEntity.ok(assetService.createAsset(dto));
    }

    @PreAuthorize("hasAuthority('ISMS_ADMIN' or 'ISMS_USER')")
    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<AssetDTO>> list(@PathVariable Long companyId) {
        return ResponseEntity.ok(assetService.listCompanyAssets(companyId));
    }

    @PreAuthorize("hasAuthority('ISMS_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<AssetDTO> update(@PathVariable String id, @RequestBody AssetDTO dto) {
        return ResponseEntity.ok(assetService.updateAsset(id, dto));
    }

    @PreAuthorize("hasAuthority('ISMS_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        assetService.deleteAsset(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export/csv/{companyId}")
    @PreAuthorize("hasAuthority('ISMS_ADMIN')")
    public void exportCsv(@PathVariable Long companyId, HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=asset-report.csv");
        assetExportService.exportCsv(companyId, response.getOutputStream());
    }

    @GetMapping("/export/pdf/{companyId}")
    @PreAuthorize("hasAuthority('ISMS_ADMIN')")
    public void exportPdf(@PathVariable Long companyId, HttpServletResponse response) throws Exception {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=asset-report.pdf");
        assetExportService.exportPdf(companyId, response.getOutputStream());
    }

    @GetMapping("/export/pdf-table/{companyId}")
    @PreAuthorize("hasAuthority('ISMS_ADMIN')")
    public void exportPdfTable(@PathVariable Long companyId, HttpServletResponse response) throws Exception {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=asset-inventory.pdf");
        assetExportService.exportPdfTable(companyId, response.getOutputStream());
    }
}
