package com.iso27001planner.repository;

import com.iso27001planner.entity.TrainingRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TrainingRecordRepository extends JpaRepository<TrainingRecord, Long> {
    List<TrainingRecord> findByEmployee_Id(UUID employeeId);
}
