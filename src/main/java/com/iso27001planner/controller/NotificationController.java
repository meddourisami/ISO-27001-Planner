package com.iso27001planner.controller;

import com.iso27001planner.dto.NotificationDTO;
import com.iso27001planner.entity.Notification;
import com.iso27001planner.mapper.NotificationMapper;
import com.iso27001planner.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @GetMapping("/{email}")
    @PreAuthorize("hasAnyAuthority('ISMS_ADMIN', 'ISMS_USER')")
    public ResponseEntity<List<NotificationDTO>> getForUser(@PathVariable String email) {
        List<Notification> notes = notificationRepository.findByTargetEmail(email);
        return ResponseEntity.ok(notificationMapper.toDTOList(notes));
    }

    @PutMapping("/{id}/mark-read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationRepository.findById(id).ifPresent(note -> {
            note.setRead(true);
            notificationRepository.save(note);
        });
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{email}/count-unread")
    @PreAuthorize("hasAnyAuthority('ISMS_ADMIN', 'ISMS_USER')")
    public ResponseEntity<Long> countUnread(@PathVariable String email) {
        return ResponseEntity.ok(notificationRepository.countUnreadByEmail(email));
    }

    @GetMapping("/admin/count-old")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<Long> countOldNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
        long count = notificationRepository.findAll().stream()
                .filter(n -> n.getSentAt().isBefore(cutoff)).count();
        return ResponseEntity.ok(count);
    }

    @PutMapping("/{email}/mark-all-read")
    @PreAuthorize("hasAnyAuthority('ISMS_ADMIN', 'ISMS_USER')")
    public ResponseEntity<Void> markAllAsRead(@PathVariable String email) {
        List<Notification> notes = notificationRepository.findByTargetEmail(email).stream()
                .filter(n -> !n.isRead())
                .peek(n -> n.setRead(true))
                .toList();

        notificationRepository.saveAll(notes);
        return ResponseEntity.ok().build();
    }
}
