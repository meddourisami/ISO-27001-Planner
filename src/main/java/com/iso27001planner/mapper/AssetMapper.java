package com.iso27001planner.mapper;

import com.iso27001planner.dto.AssetDTO;
import com.iso27001planner.entity.Asset;
import com.iso27001planner.entity.Company;
import com.iso27001planner.entity.User;
import org.springframework.stereotype.Component;

@Component
public class AssetMapper {

    public Asset toEntity(AssetDTO dto, User owner, Company company) {
        return Asset.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .type(dto.getType())
                .classification(dto.getClassification())
                .location(dto.getLocation())
                .status(dto.getStatus())
                .value(dto.getValue())
                .vulnerabilities(dto.getVulnerabilities())
                .controls(dto.getControls())
                .lastReview(dto.getLastReview())
                .relatedRisks(dto.getRelatedRisks())
                .owner(owner)
                .company(company)
                .build();
    }

    public AssetDTO toDTO(Asset asset) {
        AssetDTO dto = new AssetDTO();
        dto.setId(asset.getId());
        dto.setName(asset.getName());
        dto.setDescription(asset.getDescription());
        dto.setType(asset.getType());
        dto.setClassification(asset.getClassification());
        dto.setLocation(asset.getLocation());
        dto.setStatus(asset.getStatus());
        dto.setValue(asset.getValue());
        dto.setVulnerabilities(asset.getVulnerabilities());
        dto.setControls(asset.getControls());
        dto.setLastReview(asset.getLastReview());
        dto.setRelatedRisks(asset.getRelatedRisks());
        dto.setOwnerEmail(asset.getOwner() != null ? asset.getOwner().getEmail() : null);
        dto.setCompanyId(asset.getCompany() != null ? asset.getCompany().getId() : null);
        return dto;
    }
}
