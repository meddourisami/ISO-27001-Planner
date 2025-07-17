package com.iso27001planner.repository;

import com.iso27001planner.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByTargetEmail(String email);

    List<Notification> findByTargetEmailAndRisk_Id(String email, String riskId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.targetEmail = :email AND n.read = false")
    long countUnreadByEmail(@Param("email") String email);

    List<Notification> findByCompanyId(Long company_id);
}
