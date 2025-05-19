package com.iso27001planner.service;

import com.iso27001planner.dto.NonConformityDTO;
import com.iso27001planner.entity.Company;
import com.iso27001planner.entity.NonConformity;
import com.iso27001planner.exception.BusinessException;
import com.iso27001planner.repository.CompanyRepository;
import com.iso27001planner.repository.NonConformityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NonConformityService {

    private final NonConformityRepository repository;
    private final CompanyRepository companyRepository;

    public NonConformityDTO create(NonConformityDTO dto) {
        Company company = companyRepository.findById(dto.getCompanyId())
                .orElseThrow(() -> new BusinessException("Company not found", HttpStatus.NOT_FOUND));

        NonConformity nc = NonConformity.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .source(dto.getSource())
                .sourceReference(dto.getSourceReference())
                .dateIdentified(LocalDate.parse(dto.getDateIdentified()))
                .severity(dto.getSeverity())
                .status(dto.getStatus())
                .owner(dto.getOwner())
                .dueDate(LocalDate.parse(dto.getDueDate()))
                .relatedControls(dto.getRelatedControls())
                .relatedRisks(dto.getRelatedRisks())
                .correctiveActions(dto.getCorrectiveActions())
                .evidence(dto.getEvidence())
                .verificationStatus(dto.getVerificationStatus())
                .verificationDate(dto.getVerificationDate() != null ? LocalDate.parse(dto.getVerificationDate()) : null)
                .verifiedBy(dto.getVerifiedBy())
                .comments(dto.getComments())
                .company(company)
                .build();

        return toDTO(repository.save(nc));
    }

    public List<NonConformityDTO> listByCompany(Long companyId) {
        return repository.findByCompany_Id(companyId).stream().map(this::toDTO).toList();
    }

    private NonConformityDTO toDTO(NonConformity nc) {
        return new NonConformityDTO(
                nc.getId().toString(),
                nc.getTitle(),
                nc.getDescription(),
                nc.getSource(),
                nc.getSourceReference(),
                nc.getDateIdentified().toString(),
                nc.getSeverity(),
                nc.getStatus(),
                nc.getOwner(),
                nc.getDueDate().toString(),
                nc.getRelatedControls(),
                nc.getRelatedRisks(),
                nc.getCorrectiveActions(),
                nc.getEvidence(),
                nc.getVerificationStatus(),
                nc.getVerificationDate() != null ? nc.getVerificationDate().toString() : null,
                nc.getVerifiedBy(),
                nc.getComments(),
                nc.getCompany().getId()
        );
    }
}
