package com.iso27001planner.repository;

import com.iso27001planner.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssetRepository extends JpaRepository<Asset, String> {
    List<Asset> findByCompany_Id(Long companyId);
}
