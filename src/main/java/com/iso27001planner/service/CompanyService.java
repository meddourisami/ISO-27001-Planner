package com.iso27001planner.service;

import com.iso27001planner.dto.AdminUserCreationRequest;
import com.iso27001planner.dto.CompanyDTO;
import com.iso27001planner.dto.UpdateCompanyRequest;
import com.iso27001planner.entity.*;
import com.iso27001planner.event.AuditEvent;
import com.iso27001planner.exception.BusinessException;
import com.iso27001planner.mapper.CompanyMapper;
import com.iso27001planner.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ControlRepository controlRepository;
    private final DocumentTemplateRepository documentTemplateRepository;
    private final DocumentRepository documentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final CompanyMapper companyMapper;

    @Transactional
    public void createCompanyWithAdmin(AdminUserCreationRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("User already exists", HttpStatus.CONFLICT);
        }

        // 1. Create Company
        Company company = Company.builder()
                .name(request.getCompanyName())
                .ismsScope(request.getScope())
                .build();
        companyRepository.save(company);

        // 2. Create Admin User
        User admin = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ISMS_ADMIN)
                .company(company)
                .mfaEnabled(false)
                .build();
        userRepository.save(admin);

        // 3. Clone ISO Controls
        List<Control> controlTemplates = controlRepository.findByCompanyIsNull();
        controlTemplates.forEach(template -> {
            Control copy = Control.builder()
                    .title(template.getTitle())
                    .description(template.getDescription())
                    .status("not_implemented")
                    .lastReview(LocalDate.now())
                    .company(company)
                    .build();
            controlRepository.save(copy);
        });

        // 4. Clone Document Templates
        List<DocumentTemplate> documentTemplates = documentTemplateRepository.findByCompanyIsNull();

        documentTemplates.forEach(template -> {
            Document doc = Document.builder()
                    .title(template.getTitle())
                    .description(template.getDescription())
                    .type(template.getType())
                    .status("draft")
                    .version("1.0")
                    .fileName(template.getFileName())
                    .filePath(template.getFilePath())
                    .fileType(template.getFileType())
                    .fileSize(template.getFileSize())
                    .owner(admin.getEmail())
                    .company(company)
                    .build();

            documentRepository.save(doc);
        });
    }

    @Transactional
    public void updateCompany(UpdateCompanyRequest request, String performedBy) {
        Company company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new BusinessException("Company not found", HttpStatus.NOT_FOUND));

        company.setName(request.getName());
        company.setIsmsScope(request.getIsmsScope());

        companyRepository.save(company);

        eventPublisher.publishEvent(new AuditEvent(
                this,
                "UPDATE_COMPANY_DETAILS",
                performedBy,
                "Company",
                company.getId().toString(),
                "Updated company name/scope"
        ));
    }

    public CompanyDTO getCurrentUserCompany(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.UNAUTHORIZED));

        Company company = user.getCompany();
        if (company == null) {
            throw new BusinessException("User does not belong to a company", HttpStatus.BAD_REQUEST);
        }

        return companyMapper.toDto(company);
    }
}