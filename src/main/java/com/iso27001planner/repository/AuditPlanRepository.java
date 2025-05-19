package com.iso27001planner.repository;

import com.iso27001planner.entity.AuditPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditPlanRepository extends JpaRepository<AuditPlan, Long> {
    List<AuditPlan> findByCompany_Id(Long companyId);
}
