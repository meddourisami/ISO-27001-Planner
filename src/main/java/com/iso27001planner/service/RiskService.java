package com.iso27001planner.service;

import com.iso27001planner.dto.RiskDTO;
import com.iso27001planner.entity.*;
import com.iso27001planner.event.AuditEvent;
import com.iso27001planner.exception.BusinessException;
import com.iso27001planner.mapper.RiskMapper;
import com.iso27001planner.repository.AssetRepository;
import com.iso27001planner.repository.CompanyRepository;
import com.iso27001planner.repository.RiskRepository;
import com.iso27001planner.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RiskService {

    private final RiskRepository riskRepository;
    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final RiskMapper riskMapper;
    private final ApplicationEventPublisher eventPublisher;
    private RiskScoringStrategy strategy;

    public RiskDTO getRiskById(String id) {
        Risk risk = riskRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new BusinessException("Risk not found", HttpStatus.NOT_FOUND));
        return riskMapper.toDTO(risk);
    }

    public RiskDTO createRisk(RiskDTO dto) {
        Asset asset = null;
        if (dto.getAssetId() != null && !dto.getAssetId().isBlank()) {
            asset = assetRepository.findById(dto.getAssetId())
                    .orElseThrow(() -> new BusinessException("Asset not found", HttpStatus.NOT_FOUND));
        }

        User owner = userRepository.findByEmail(dto.getOwnerEmail())
                .orElseThrow(() -> new BusinessException("Owner not found", HttpStatus.NOT_FOUND));

        Company company = companyRepository.findById(dto.getCompanyId())
                .orElseThrow(() -> new BusinessException("Company not found", HttpStatus.NOT_FOUND));

        Risk risk = riskMapper.toEntity(dto, asset, owner, company);
        String severity = calculateSeverity(dto.getLikelihood(), dto.getImpact());
        risk.setSeverity(severity);

        Risk saved = riskRepository.save(risk);

        eventPublisher.publishEvent(new AuditEvent(
                this,
                "CREATE_RISK",
                 getCurrentUserEmail(),
                "Risk",
                 saved.getId(),
                "New risk added: " + saved.getTitle()
        ));

        return riskMapper.toDTO(saved);
    }

    public List<RiskDTO> listCompanyRisks(Long companyId) {
        return riskRepository.findByCompany_Id(companyId).stream()
                .map(riskMapper::toDTO)
                .toList();
    }

    public void deleteRisk(String id) {
        Risk risk = riskRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Risk not found", HttpStatus.NOT_FOUND));

        riskRepository.deleteById(UUID.fromString(id));

        eventPublisher.publishEvent(new AuditEvent(
                this,
                "DELETE_RISK",
                 getCurrentUserEmail(),
                "Risk",
                 risk.getId(),
                "Deleted risk: " + risk.getTitle()
        ));
    }

    @Transactional
    public RiskDTO updateRisk(String id, RiskDTO dto) {
        try {
            System.out.println("🔁 Attempting to update risk with ID: " + id);
            System.out.println("📥 Incoming DTO: " + dto);

            Risk risk = riskRepository.findById(id)
                    .orElseThrow(() -> new BusinessException("Risk not found", HttpStatus.NOT_FOUND));

            Asset asset = null;
            if (dto.getAssetId() != null && !dto.getAssetId().isBlank()) {
                asset = assetRepository.findById(dto.getAssetId())
                        .orElseThrow(() -> new BusinessException("Asset not found", HttpStatus.NOT_FOUND));
            }

            // Logging each update for clarity
            System.out.println("🔧 Updating Risk fields...");
            risk.setTitle(dto.getTitle());
            risk.setDescription(dto.getDescription());
            risk.setAsset(asset);
            risk.setThreat(dto.getThreat());
            risk.setVulnerability(dto.getVulnerability());
            risk.setLikelihood(dto.getLikelihood());
            risk.setImpact(dto.getImpact());

            String severity = calculateSeverity(dto.getLikelihood(), dto.getImpact());
            System.out.println("📊 Calculated Severity: " + severity);
            risk.setSeverity(severity);

            risk.setStatus(dto.getStatus());
            risk.setTreatment(dto.getTreatment());
            risk.setControls(dto.getControls());

            if (dto.getDueDate() != null && !dto.getDueDate().isBlank()) {
                risk.setDueDate(dto.getDueDate()); // assuming it's already in String form
            }

            Risk updated = riskRepository.save(risk);
            System.out.println("✅ Risk updated successfully: " + updated.getId());

            // Audit Logging
            eventPublisher.publishEvent(new AuditEvent(
                    this,
                    "UPDATE_RISK",
                    getCurrentUserEmail(),
                    "Risk",
                    updated.getId(),
                    "Updated risk: " + updated.getTitle()
            ));

            return riskMapper.toDTO(updated);

        } catch (IllegalArgumentException e) {
            System.err.println("❌ Invalid UUID: " + id);
            throw new BusinessException("Invalid risk ID format", HttpStatus.BAD_REQUEST);
        } catch (BusinessException be) {
            System.err.println("❌ Business error: " + be.getMessage());
            throw be;
        } catch (Exception e) {
            e.printStackTrace(); // Always log full error
            throw new BusinessException("Failed to update risk: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String calculateSeverity(String likelihood, String impact) {
        Map<String, Integer> scale = Map.of(
                "low", 1,
                "medium", 2,
                "high", 3,
                "critical", 4
        );

        int l = scale.getOrDefault(likelihood.toLowerCase(), 0);
        int i = scale.getOrDefault(impact.toLowerCase(), 0);
        int score = l * i;

        if (score <= 4) return "Low";
        else if (score <= 8) return "Medium";
        else if (score <= 12) return "High";
        else return "Critical";
    }

    private final Map<String, Map<String, String>> severityMatrix = Map.of(
            "low", Map.of("low", "Low", "medium", "Low", "high", "Medium", "critical", "Medium"),
            "medium", Map.of("low", "Low", "medium", "Medium", "high", "High", "critical", "High"),
            "high", Map.of("low", "Medium", "medium", "High", "high", "Critical", "critical", "Critical"),
            "critical", Map.of("low", "Medium", "medium", "High", "high", "Critical", "critical", "Critical")
    );

    private String calculateSeverityISO(String likelihood, String impact) {
        String l = likelihood.toLowerCase();
        String i = impact.toLowerCase();
        return severityMatrix.getOrDefault(l, Map.of()).getOrDefault(i, "Unknown");
    }

    public String computeSeverity(String likelihood, String impact) {
        return switch (strategy) {
            case QUANTITATIVE -> calculateSeverity(likelihood, impact);
            case QUALITATIVE, MATRIX -> calculateSeverityISO(likelihood, impact);
        };
    }

    public Map<String, Map<String, Long>> getRiskMatrix(Long companyId) {
        List<Risk> risks = riskRepository.findByCompany_Id(companyId);

        Map<String, Map<String, Long>> matrix = new TreeMap<>();

        for (Risk r : risks) {
            String likelihood = r.getLikelihood();
            String impact = r.getImpact();

            matrix.putIfAbsent(likelihood, new TreeMap<>());
            Map<String, Long> impactMap = matrix.get(likelihood);
            impactMap.put(impact, impactMap.getOrDefault(impact, 0L) + 1);
        }

        return matrix;
    }

    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";
    }

}
