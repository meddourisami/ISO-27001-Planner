package com.iso27001planner.repository;

import com.iso27001planner.entity.NonConformity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NonConformityRepository extends JpaRepository<NonConformity, UUID> {
    List<NonConformity> findByCompany_Id(Long companyId);
}
