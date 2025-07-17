package com.iso27001planner.service;

import com.iso27001planner.entity.Notification;
import com.iso27001planner.entity.Risk;
import com.iso27001planner.entity.User;
import com.iso27001planner.repository.NotificationRepository;
import com.iso27001planner.repository.RiskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RiskReminderScheduler {

    private final RiskRepository riskRepository;
    private final EmailSenderService emailSenderService;
    private final NotificationRepository notificationRepository;

    //@Scheduled(cron = "0 0 7 * * ?") // Run daily at 7 AM
    public void sendRemindersForTomorrow() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDateTime now = LocalDateTime.now();

        List<Risk> risksDueTomorrow = riskRepository.findAll().stream()
                .filter(r -> r.getDueDate() != null && tomorrow.toString().equals(r.getDueDate()))
                .toList();

        for (Risk risk : risksDueTomorrow) {
            User owner = risk.getOwner();
            if (owner == null || owner.getEmail() == null) continue;

            String message = "⏰ Reminder: The risk \"" + risk.getTitle() + "\" is due tomorrow (" + risk.getDueDate() + ").";
            String email = owner.getEmail();

            // 🛡️ Prevent duplicate reminders
            boolean alreadyRemindedToday = notificationRepository
                    .findByTargetEmailAndRisk_Id(email, risk.getId()).stream()
                    .anyMatch(n -> n.getSentAt().toLocalDate().equals(LocalDate.now()));

            if (alreadyRemindedToday) continue;

            // ✅ Send the reminder email
            emailSenderService.sendOtp(email, message);

            // 📝 Log it to DB
            Notification log = Notification.builder()
                    .type("risk")
                    .title("Upcoming Risk Due")
                    .description("Reminder: The risk \"" + risk.getTitle() + "\" is due on " + risk.getDueDate() + ".")
                    .category("reminder")
                    .targetEmail(email)
                    .risk(risk)
                    .sentAt(now)
                    .read(false)
                    .build();

            notificationRepository.save(log);
        }
    }

    //@Scheduled(cron = "0 0 8 ? * MON") // Every Monday at 8 AM
    public void sendWeeklyDigest() {
        LocalDate today = LocalDate.now();
        LocalDate endOfWeek = today.plusDays(6);

        List<Risk> risks = riskRepository.findAll();

        Map<String, List<Risk>> risksPerUser = new HashMap<>();

        // Group risks by owner email that are due this week
        for (Risk r : risks) {
            if (r.getDueDate() == null) continue;

            LocalDate due = LocalDate.parse(r.getDueDate());
            if (!due.isBefore(today) && !due.isAfter(endOfWeek) && r.getOwner() != null) {
                String email = r.getOwner().getEmail();
                risksPerUser.computeIfAbsent(email, e -> new ArrayList<>()).add(r);
            }
        }

        // Send summary per user
        for (Map.Entry<String, List<Risk>> entry : risksPerUser.entrySet()) {
            String email = entry.getKey();
            List<Risk> userRisks = entry.getValue();

            StringBuilder message = new StringBuilder("📋 Weekly Risk Summary:\n\n");

            for (Risk r : userRisks) {
                message.append("• ").append(r.getTitle())
                        .append(" (Due: ").append(r.getDueDate()).append(")\n");
            }

            emailSenderService.sendOtp(email, message.toString());

            // Optional: log digest notification
            Notification notification = Notification.builder()
                    .type("risk")
                    .category("digest")
                    .title("Weekly Risk Digest")
                    .description("You have " + userRisks.size() + " risks due this week.")
                    .targetEmail(email)
                    .sentAt(LocalDateTime.now())
                    .read(false)
                    .build();

            notificationRepository.save(notification);
        }
    }

    //@Scheduled(cron = "0 0 2 * * ?") // Every day at 2 AM
    public void purgeOldNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
        List<Notification> old = notificationRepository.findAll().stream()
                .filter(n -> n.getSentAt().isBefore(cutoff))
                .toList();

        if (!old.isEmpty()) {
            notificationRepository.deleteAll(old);
            System.out.println("Deleted " + old.size() + " old notifications");
        }
    }
}
