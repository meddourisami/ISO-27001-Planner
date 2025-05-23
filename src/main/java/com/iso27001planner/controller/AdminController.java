package com.iso27001planner.controller;

import com.iso27001planner.dto.AdminUserCreationRequest;
import com.iso27001planner.entity.Company;
import com.iso27001planner.entity.Role;
import com.iso27001planner.entity.User;
import com.iso27001planner.repository.CompanyRepository;
import com.iso27001planner.repository.UserRepository;
import com.iso27001planner.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class AdminController {

    private final CompanyService companyService;

    @PostMapping("/create-isms-admin")
    public ResponseEntity<String> createIsmsAdmin(@RequestBody AdminUserCreationRequest request) {
        companyService.createCompanyWithAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("ISMS Admin and Company created successfully.");
    }
}
