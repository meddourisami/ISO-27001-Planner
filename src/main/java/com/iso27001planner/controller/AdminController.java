package com.iso27001planner.controller;

import com.iso27001planner.dto.AdminUserCreationRequest;
import com.iso27001planner.entity.Company;
import com.iso27001planner.entity.Role;
import com.iso27001planner.entity.User;
import com.iso27001planner.repository.CompanyRepository;
import com.iso27001planner.repository.UserRepository;
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

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/create-isms-admin")
    public ResponseEntity<String> createIsmsAdmin(@RequestBody AdminUserCreationRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User already exists");
        }

        Company company = new Company();
        company.setName(request.getCompanyName());
        company.setIsmsScope(request.getScope());
        companyRepository.save(company);

        User admin = new User();
        admin.setEmail(request.getEmail());
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        admin.setRole(Role.ISMS_ADMIN);
        admin.setCompany(company);
        admin.setMfaEnabled(false);

        userRepository.save(admin);

        return ResponseEntity.status(HttpStatus.CREATED).body("ISMS Admin created with company.");
    }
}
