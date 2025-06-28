package com.iso27001planner.controller;

import com.iso27001planner.dto.CompanyDTO;
import com.iso27001planner.dto.UpdateCompanyRequest;
import com.iso27001planner.exception.BusinessException;
import com.iso27001planner.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

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
}
