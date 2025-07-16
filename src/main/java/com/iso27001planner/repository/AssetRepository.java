package com.iso27001planner.repository;

import com.iso27001planner.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, String> {
    Optional<Asset> findById(String id);

    List<Asset> findByCompany_Id(Long companyId);
}
