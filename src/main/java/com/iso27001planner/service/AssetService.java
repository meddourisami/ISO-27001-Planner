package com.iso27001planner.service;

import com.iso27001planner.dto.AssetDTO;
import com.iso27001planner.entity.Asset;
import com.iso27001planner.entity.Company;
import com.iso27001planner.entity.User;
import com.iso27001planner.event.AuditEvent;
import com.iso27001planner.exception.BusinessException;
import com.iso27001planner.mapper.AssetMapper;
import com.iso27001planner.repository.AssetRepository;
import com.iso27001planner.repository.CompanyRepository;
import com.iso27001planner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final AssetMapper assetMapper;
    private final ApplicationEventPublisher eventPublisher;

    public AssetDTO getAssetById(String id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Asset not found", HttpStatus.NOT_FOUND));

        eventPublisher.publishEvent(new AuditEvent(
                this,
                "VIEW_ASSET",
                getCurrentUserEmail(),
                "Asset",
                id,
                "Viewed asset details"
        ));
        return assetMapper.toDTO(asset);
    }

    public AssetDTO createAsset(AssetDTO dto) {
        User owner = userRepository.findByEmail(dto.getOwnerEmail())
                .orElseThrow(() -> new BusinessException("Owner not found", HttpStatus.NOT_FOUND));

        Company company = companyRepository.findById(dto.getCompanyId())
                .orElseThrow(() -> new BusinessException("Company not found", HttpStatus.NOT_FOUND));

        Asset asset = assetMapper.toEntity(dto, owner, company);
        Asset saved = assetRepository.save(asset);

        eventPublisher.publishEvent(new AuditEvent(
                this,
                "CREATE_ASSET",
                getCurrentUserEmail(),
                "Asset",
                saved.getId().toString(),
                "Created asset: " + saved.getName()
        ));

        return assetMapper.toDTO(saved);
    }

    public List<AssetDTO> listCompanyAssets(Long companyId) {
        return assetRepository.findByCompany_Id(companyId).stream()
                .map(assetMapper::toDTO)
                .toList();
    }

    public void deleteAsset(String id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Asset not found", HttpStatus.NOT_FOUND));

        assetRepository.delete(asset);

        eventPublisher.publishEvent(new AuditEvent(
                this,
                "DELETE_ASSET",
                getCurrentUserEmail(),
                "Asset",
                id,
                "Deleted asset: " + asset.getName()
        ));
    }

    public AssetDTO updateAsset(String id, AssetDTO dto) {
        Asset existing = assetRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Asset not found", HttpStatus.NOT_FOUND));

        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());
        existing.setType(dto.getType());
        existing.setClassification(dto.getClassification());
        existing.setLocation(dto.getLocation());
        existing.setStatus(dto.getStatus());
        existing.setValue(dto.getValue());
        existing.setVulnerabilities(dto.getVulnerabilities());
        existing.setControls(dto.getControls());
        existing.setLastReview(dto.getLastReview());
        existing.setRelatedRisks(dto.getRelatedRisks());

        Asset updated = assetRepository.save(existing);

        eventPublisher.publishEvent(new AuditEvent(
                this,
                "UPDATE_ASSET",
                getCurrentUserEmail(),
                "Asset",
                updated.getId(),
                "Updated asset: " + updated.getName()
        ));

        return assetMapper.toDTO(updated);
    }

    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";
    }
}
