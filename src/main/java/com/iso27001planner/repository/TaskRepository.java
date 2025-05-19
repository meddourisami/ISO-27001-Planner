package com.iso27001planner.repository;

import com.iso27001planner.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findByCompany_Id(Long companyId);
    List<Task> findByAssignee(String assignee);

    @Query("""
    SELECT t FROM Task t
    WHERE t.progress < 100
      AND t.risk IS NOT NULL
      AND (t.risk.severity = 'High' OR t.risk.severity = 'Critical')
      AND t.dueDate <= :threshold
""")
    List<Task> findUrgentHighRiskTasks(@Param("threshold") LocalDate threshold);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.risk.id = :riskId")
    int countByRiskId(@Param("riskId") Long riskId);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.risk.id = :riskId AND t.progress = 100")
    int countCompletedByRiskId(@Param("riskId") Long riskId);
}

