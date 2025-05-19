package com.iso27001planner.repository;

import com.iso27001planner.entity.Training;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TrainingRepository extends JpaRepository<Training, UUID> {
    List<Training> findByCompany_Id(Long companyId);
}
