package com.iso27001planner.service;

import com.iso27001planner.dto.ControlDTO;
import com.iso27001planner.dto.controlUpdateRequest;
import com.iso27001planner.entity.Company;
import com.iso27001planner.entity.Control;
import com.iso27001planner.event.AuditEvent;
import com.iso27001planner.exception.BusinessException;
import com.iso27001planner.repository.CompanyRepository;
import com.iso27001planner.repository.ControlRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ControlService {

    private final ControlRepository controlRepo;
    private final CompanyRepository companyRepo;
    private final ApplicationEventPublisher eventPublisher;

    public ControlDTO create(ControlDTO dto) {
        Company company = companyRepo.findById(dto.getCompanyId())
                .orElseThrow(() -> new BusinessException("Company not found", HttpStatus.NOT_FOUND));

        Control control = Control.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .status(dto.getStatus())
                .evidence(dto.getEvidence())
                .lastReview(LocalDate.parse(dto.getLastReview()))
                .company(company)
                .build();

        return toDTO(controlRepo.save(control));
    }

    public List<ControlDTO> listByCompany(Long companyId) {
        return controlRepo.findByCompany_Id(companyId).stream()
                .map(this::toDTO).toList();
    }

    @Transactional
    public void updateStatusAndEvidence(String id, controlUpdateRequest request) {
        Control control = controlRepo.findById(UUID.fromString(id))
                .orElseThrow(() -> new BusinessException("Control not found", HttpStatus.NOT_FOUND));

        control.setStatus(request.getStatus());
        control.setEvidence(request.getEvidence());
        control.setLastReview(LocalDate.now());

        controlRepo.save(control);
        eventPublisher.publishEvent(new AuditEvent(
                this, "UPDATE_CONTROL_STATUS", getCurrentUserEmail(), "Control", control.getId().toString(), "Status updated to: " + control.getStatus()
        ));
    }

    private ControlDTO toDTO(Control c) {
        return new ControlDTO(
                c.getId().toString(),
                c.getTitle(),
                c.getDescription(),
                c.getStatus(),
                c.getEvidence(),
                c.getLastReview().toString(),
                c.getCompany().getId()
        );
    }

    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";
    }
}
