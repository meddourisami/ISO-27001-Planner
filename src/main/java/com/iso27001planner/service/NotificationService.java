package com.iso27001planner.service;

import com.iso27001planner.entity.Company;
import com.iso27001planner.entity.Notification;
import com.iso27001planner.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;

    public void notify(String email, String type, String category, String title, String description, Company company) {
        Notification notification = Notification.builder()
                .type(type)
                .category(category)
                .title(title)
                .description(description)
                .targetEmail(email)
                .sentAt(LocalDateTime.now())
                .read(false)
                .company(company)
                .build();

        repository.save(notification);
    }

    // 🔥 High Risk Notification
    public void notifyHighRisk(String email, String riskTitle, Company company) {
        notify(email, "risk", "alert", "High Risk Identified",
                "A new high risk has been identified: " + riskTitle, company);
    }

    // 📅 Upcoming Audit Notification
    public void notifyUpcomingAudit(String email, LocalDate date, Company company) {
        notify(email, "audit", "reminder", "Upcoming Audit",
                "Annual Internal Audit scheduled for " + date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")) + ".", company);
    }

    // ✅ Task Deadline Notification
    public void notifyTaskDueSoon(String email, String taskTitle, long daysLeft, Company company) {
        notify(email, "task", "reminder", "Task Due Soon",
                taskTitle + " is due in " + daysLeft + " days.", company);
    }

    // 📄 Document Approval
    public void notifyDocumentApproval(String email, String docTitle, Company company) {
        notify(email, "document", "action", "Document Approval Required",
                docTitle + " requires your approval.", company);
    }

    // ⚠️ Compliance Control Overdue
    public void notifyComplianceOverdue(String email, String controlTitle, Company company) {
        notify(email, "compliance", "alert", "Control Implementation Overdue",
                controlTitle + " implementation is overdue.", company);
    }

    // 🎓 New Training
    public void notifyTrainingAvailable(String email, String trainingTitle, Company company) {
        notify(email, "training", "info", "New Training Available",
                trainingTitle + " training is now available.", company);
    }
}
