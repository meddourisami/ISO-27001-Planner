package com.iso27001planner.controller;

import com.iso27001planner.dto.ControlDTO;
import com.iso27001planner.dto.controlUpdateRequest;
import com.iso27001planner.service.ControlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/controls")
@RequiredArgsConstructor
public class ControlController {

    private final ControlService controlService;

    @PostMapping
    public ResponseEntity<ControlDTO> create(@RequestBody ControlDTO dto) {
        return ResponseEntity.ok(controlService.create(dto));
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<ControlDTO>> listByCompany(@PathVariable Long companyId) {
        return ResponseEntity.ok(controlService.listByCompany(companyId));
    }

    @PutMapping("/{id}/update-status")
    @PreAuthorize("hasAnyAuthority('ISMS_ADMIN')")
    public ResponseEntity<String> updateStatusAndEvidence(
            @PathVariable String id,
            @RequestBody controlUpdateRequest request
    ) {
        controlService.updateStatusAndEvidence(id, request);
        return ResponseEntity.ok("Control updated successfully.");
    }
}
