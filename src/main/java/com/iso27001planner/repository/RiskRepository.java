package com.iso27001planner.repository;

import com.iso27001planner.entity.Control;
import com.iso27001planner.entity.Risk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RiskRepository extends JpaRepository<Risk, UUID> {
    Optional<Risk> findById(String id);
    List<Risk> findByCompany_Id(Long companyId);
    List<Risk> findByAsset_Id(String assetId);
    List<Risk> findAllById(String id);
}
