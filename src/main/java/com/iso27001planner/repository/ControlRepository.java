package com.iso27001planner.repository;

import com.iso27001planner.entity.Control;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ControlRepository extends JpaRepository<Control, UUID> {

    // Find controls by company
    List<Control> findByCompany_Id(Long companyId);

    // Find template controls (not yet assigned to a company)
    List<Control> findByCompanyIsNull();

    List<Control> findByStatusAndCompany_Id(String status, Long companyId);

    List<Control> findByLastReviewBefore(LocalDate date);
}
