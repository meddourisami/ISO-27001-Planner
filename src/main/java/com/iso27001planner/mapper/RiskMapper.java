package com.iso27001planner.mapper;

import com.iso27001planner.dto.RiskDTO;
import com.iso27001planner.entity.Asset;
import com.iso27001planner.entity.Company;
import com.iso27001planner.entity.Risk;
import com.iso27001planner.entity.User;
import org.springframework.stereotype.Component;

@Component
public class RiskMapper {

    public Risk toEntity(RiskDTO dto, Asset asset, User owner, Company company) {
        return Risk.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .asset(asset)
                .threat(dto.getThreat())
                .vulnerability(dto.getVulnerability())
                .likelihood(dto.getLikelihood())
                .impact(dto.getImpact())
                .severity(dto.getSeverity())
                .status(dto.getStatus())
                .treatment(dto.getTreatment())
                .controls(dto.getControls())
                .dueDate(dto.getDueDate())
                .owner(owner)
                .company(company)
                .build();
    }

    public RiskDTO toDTO(Risk risk) {
        RiskDTO dto = new RiskDTO();
        dto.setId(risk.getId());
        dto.setTitle(risk.getTitle());
        dto.setDescription(risk.getDescription());
        dto.setAssetId(risk.getAsset() != null ? risk.getAsset().getId() : null);
        dto.setThreat(risk.getThreat());
        dto.setVulnerability(risk.getVulnerability());
        dto.setLikelihood(risk.getLikelihood());
        dto.setImpact(risk.getImpact());
        dto.setSeverity(risk.getSeverity());
        dto.setStatus(risk.getStatus());
        dto.setTreatment(risk.getTreatment());
        dto.setControls(risk.getControls());
        dto.setDueDate(risk.getDueDate());
        dto.setOwnerEmail(risk.getOwner() != null ? risk.getOwner().getEmail() : null);
        dto.setCompanyId(risk.getCompany() != null ? risk.getCompany().getId() : null);
        return dto;
    }
}
