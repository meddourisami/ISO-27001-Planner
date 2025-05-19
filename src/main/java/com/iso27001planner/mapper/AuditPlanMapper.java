package com.iso27001planner.mapper;

import com.iso27001planner.dto.AuditPlanDTO;
import com.iso27001planner.entity.AuditPlan;
import com.iso27001planner.entity.Company;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class AuditPlanMapper {

    public AuditPlanDTO toDTO(AuditPlan plan) {
        return new AuditPlanDTO(
                plan.getId(),
                plan.getTitle(),
                plan.getDescription(),
                plan.getType(),
                plan.getScope(),
                plan.getStartDate().toString(),
                plan.getEndDate() != null ? plan.getEndDate().toString() : null,
                plan.getStatus(),
                plan.getAuditor(),
                plan.getFindings(),
                plan.getNonConformities(),
                plan.getObservations(),
                plan.getRecommendations(),
                plan.getCompany() != null ? plan.getCompany().getId() : null
        );
    }

    public AuditPlan toEntity(AuditPlanDTO dto, Company company) {
        return AuditPlan.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .type(dto.getType())
                .scope(dto.getScope())
                .startDate(LocalDate.parse(dto.getDate()))
                .endDate(dto.getEndDate() != null ? LocalDate.parse(dto.getEndDate()) : null)
                .status(dto.getStatus())
                .auditor(dto.getAuditor())
                .findings(dto.getFindings())
                .nonConformities(dto.getNonConformities())
                .observations(dto.getObservations())
                .recommendations(dto.getRecommendations())
                .company(company)
                .build();
    }
}
