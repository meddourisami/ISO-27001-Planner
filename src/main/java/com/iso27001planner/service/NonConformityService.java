package com.iso27001planner.service;

import com.iso27001planner.dto.NonConformityDTO;
import com.iso27001planner.entity.Company;
import com.iso27001planner.entity.Control;
import com.iso27001planner.entity.NonConformity;
import com.iso27001planner.entity.Risk;
import com.iso27001planner.event.AuditEvent;
import com.iso27001planner.exception.BusinessException;
import com.iso27001planner.repository.CompanyRepository;
import com.iso27001planner.repository.ControlRepository;
import com.iso27001planner.repository.NonConformityRepository;
import com.iso27001planner.repository.RiskRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NonConformityService {

    private final NonConformityRepository repository;
    private final CompanyRepository companyRepository;
    private final ControlRepository controlRepo;
    private final RiskRepository riskRepo;
    private final ApplicationEventPublisher eventPublisher;

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

        NonConformity saved = repository.save(nc);

        eventPublisher.publishEvent(new AuditEvent(
                this,
                "CREATE_NONCONFORMITY",
                 getCurrentUserEmail(),
                "NonConformity",
                 saved.getId().toString(),
                "Added non-conformity: " + nc.getTitle()
        ));

        return toDTO(saved);
    }

    public List<NonConformityDTO> listByCompany(Long companyId) {
        return repository.findByCompany_Id(companyId).stream().map(this::toDTO).toList();
    }

    @Transactional
    public NonConformityDTO update(String id, NonConformityDTO dto) {
        log.debug("🔍 Starting update of NonConformity: {}", id);
        log.debug("📦 Incoming DTO: {}", dto);

        UUID ncId;
        try {
            ncId = UUID.fromString(id.trim());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid UUID format for NonConformity ID", HttpStatus.BAD_REQUEST);
        }

        NonConformity nc = repository.findById(ncId)
                .orElseThrow(() -> new BusinessException("Non-Conformity not found with ID: " + id, HttpStatus.NOT_FOUND));

        try {
            // Update fields
            nc.setTitle(dto.getTitle());
            nc.setDescription(dto.getDescription());
            nc.setSource(dto.getSource());
            nc.setSourceReference(dto.getSourceReference());
            nc.setDateIdentified(LocalDate.parse(dto.getDateIdentified()));
            nc.setSeverity(dto.getSeverity());
            nc.setStatus(dto.getStatus());
            nc.setOwner(dto.getOwner());
            nc.setDueDate(LocalDate.parse(dto.getDueDate()));
            nc.setCorrectiveActions(dto.getCorrectiveActions());
            nc.setEvidence(dto.getEvidence());
            nc.setVerificationStatus(dto.getVerificationStatus());
            if (dto.getVerificationDate() != null && !dto.getVerificationDate().isBlank()) {
                nc.setVerificationDate(LocalDate.parse(dto.getVerificationDate()));
            } else {
                nc.setVerificationDate(null);
            }
            nc.setVerifiedBy(dto.getVerifiedBy());
            nc.setComments(dto.getComments());

            // Fetch related controls
            log.debug("🔗 Resolving relatedControls: {}", dto.getRelatedControls());
            List<UUID> controlIds = dto.getRelatedControls().stream()
                    .map(UUID::fromString)
                    .toList();
            List<Control> controls = controlRepo.findAllById(controlIds);
            nc.setRelatedControls(controls);
            log.debug("✅ Fetched Controls: {}", controls.stream().map(Control::getId).toList());

            // Fetch related risks
            log.debug("🔗 Resolving relatedRisks: {}", dto.getRelatedRisks());
            List<String> riskIds = dto.getRelatedRisks();
            List<Risk> risks = riskRepo.findAllById(riskIds.toString());
            nc.setRelatedRisks(risks);
            log.debug("✅ Fetched Risks: {}", risks.stream().map(Risk::getId).toList());

            // Optional: Check status triggers
//            if ("verified".equalsIgnoreCase(nc.getStatus()) || "resolved".equalsIgnoreCase(nc.getStatus())) {
//                notificationService.createNotification(
//                        nc.getCompany(),
//                        "nonconformity",
//                        "Status changed to: " + nc.getStatus(),
//                        "Non-conformity '" + nc.getTitle() + "' was updated.",
//                        false
//                );
//            }

            eventPublisher.publishEvent(new AuditEvent(
                    this,
                    "UPDATE_NONCONFORMITY",
                    getCurrentUserEmail(),
                    "NonConformity",
                    nc.getId().toString(),
                    "Updated non-conformity: " + nc.getTitle()
            ));

            NonConformityDTO result = toDTO(repository.save(nc));
            log.debug("📤 Updated NonConformity DTO: {}", result);
            return result;

        } catch (Exception e) {
            log.error("❌ Failed to update NonConformity: {}", e.getMessage(), e);
            throw new BusinessException("Update failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public void delete(String id) {
        if (!repository.existsById(UUID.fromString(id))) {
            throw new BusinessException("Non-Conformity not found", HttpStatus.NOT_FOUND);
        }
        repository.deleteById(UUID.fromString(id));

        eventPublisher.publishEvent(new AuditEvent(
                this,
                "DELETE_NONCONFORMITY",
                getCurrentUserEmail(),
                "NonConformity",
                id,
                "Deleted non-conformity"
        ));
    }

    private NonConformityDTO toDTO(NonConformity nc) {
        List<String> controlIds = nc.getRelatedControls() != null
                ? nc.getRelatedControls().stream()
                .map(c -> c.getId().toString())
                .toList()
                : List.of();

        List<String> riskIds = nc.getRelatedRisks() != null
                ? nc.getRelatedRisks().stream()
                .map(r -> r.getId().toString())  // force String
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

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
