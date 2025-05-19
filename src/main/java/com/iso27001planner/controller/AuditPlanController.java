package com.iso27001planner.controller;

import com.iso27001planner.dto.AuditPlanDTO;
import com.iso27001planner.entity.AuditPlan;
import com.iso27001planner.entity.Company;
import com.iso27001planner.exception.BusinessException;
import com.iso27001planner.mapper.AuditPlanMapper;
import com.iso27001planner.repository.AuditPlanRepository;
import com.iso27001planner.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audits")
@RequiredArgsConstructor
public class AuditPlanController {

    private final AuditPlanRepository repository;
    private final AuditPlanMapper mapper;
    private final CompanyRepository companyRepository;

    @PostMapping
    @PreAuthorize("hasAuthority('ISMS_ADMIN')")
    public ResponseEntity<AuditPlanDTO> create(@RequestBody AuditPlanDTO dto) {
        Company company = companyRepository.findById(dto.getCompanyId())
                .orElseThrow(() -> new BusinessException("Company not found", HttpStatus.NOT_FOUND));
        AuditPlan plan = mapper.toEntity(dto, company);
        return ResponseEntity.ok(mapper.toDTO(repository.save(plan)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ISMS_ADMIN', 'ISMS_USER')")
    public ResponseEntity<AuditPlanDTO> getOne(@PathVariable Long id) {
        AuditPlan plan = repository.findById(id)
                .orElseThrow(() -> new BusinessException("Audit not found", HttpStatus.NOT_FOUND));
        return ResponseEntity.ok(mapper.toDTO(plan));
    }

    @GetMapping("/company/{companyId}")
    @PreAuthorize("hasAnyAuthority('ISMS_ADMIN', 'ISMS_USER')")
    public ResponseEntity<List<AuditPlanDTO>> getAll(@PathVariable Long companyId) {
        List<AuditPlan> plans = repository.findByCompany_Id(companyId);
        return ResponseEntity.ok(plans.stream().map(mapper::toDTO).toList());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ISMS_ADMIN')")
    public ResponseEntity<AuditPlanDTO> update(@PathVariable Long id, @RequestBody AuditPlanDTO dto) {
        AuditPlan existing = repository.findById(id)
                .orElseThrow(() -> new BusinessException("Audit not found", HttpStatus.NOT_FOUND));
        Company company = companyRepository.findById(dto.getCompanyId())
                .orElseThrow(() -> new BusinessException("Company not found", HttpStatus.NOT_FOUND));
        AuditPlan updated = mapper.toEntity(dto, company);
        updated.setId(id);
        return ResponseEntity.ok(mapper.toDTO(repository.save(updated)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ISMS_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
