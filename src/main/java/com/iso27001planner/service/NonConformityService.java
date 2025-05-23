package com.iso27001planner.service;

import com.iso27001planner.dto.NonConformityDTO;
import com.iso27001planner.entity.Company;
import com.iso27001planner.entity.Control;
import com.iso27001planner.entity.NonConformity;
import com.iso27001planner.entity.Risk;
import com.iso27001planner.exception.BusinessException;
import com.iso27001planner.repository.CompanyRepository;
import com.iso27001planner.repository.ControlRepository;
import com.iso27001planner.repository.NonConformityRepository;
import com.iso27001planner.repository.RiskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NonConformityService {

    private final NonConformityRepository repository;
    private final CompanyRepository companyRepository;
    private final ControlRepository controlRepo;
    private final RiskRepository riskRepo;

    public NonConformityDTO create(NonConformityDTO dto) {
        Company company = companyRepository.findById(dto.getCompanyId())
                .orElseThrow(() -> new BusinessException("Company not found", HttpStatus.NOT_FOUND));

        List<Control> controls = controlRepo.findAllById(dto.getRelatedControls().stream()
                .map(UUID::fromString).toList());

        List<Risk> risks = riskRepo.findAllById(dto.getRelatedRisks().stream()
                .map(UUID::fromString).toList().toString());


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
                .correctiveActions(dto.getCorrectiveActions())
                .evidence(dto.getEvidence())
                .verificationStatus(dto.getVerificationStatus())
                .verificationDate(dto.getVerificationDate() != null ? LocalDate.parse(dto.getVerificationDate()) : null)
                .verifiedBy(dto.getVerifiedBy())
                .comments(dto.getComments())
                .company(company)
                .build();

        nc.setRelatedControls(controls);
        nc.setRelatedRisks(risks);

        return toDTO(repository.save(nc));
    }

    public List<NonConformityDTO> listByCompany(Long companyId) {
        return repository.findByCompany_Id(companyId).stream().map(this::toDTO).toList();
    }

    private NonConformityDTO toDTO(NonConformity nc) {
        List<String> controlIds = nc.getRelatedControls() != null
                ? nc.getRelatedControls().stream()
                .map(c -> c.getId().toString())
                .toList()
                : List.of();

        List<String> riskIds = nc.getRelatedRisks() != null
                ? nc.getRelatedRisks().stream()
                .map(Risk::getId)
                .toList()
                : List.of();

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
                controlIds,              // stringified control IDs
                riskIds,                 // stringified risk IDs
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
