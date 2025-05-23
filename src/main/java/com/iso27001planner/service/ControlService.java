package com.iso27001planner.service;

import com.iso27001planner.dto.ControlDTO;
import com.iso27001planner.entity.Company;
import com.iso27001planner.entity.Control;
import com.iso27001planner.exception.BusinessException;
import com.iso27001planner.repository.CompanyRepository;
import com.iso27001planner.repository.ControlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ControlService {

    private final ControlRepository controlRepo;
    private final CompanyRepository companyRepo;

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
}
