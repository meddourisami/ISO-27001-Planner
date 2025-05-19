package com.iso27001planner.service;

import com.iso27001planner.entity.Notification;
import com.iso27001planner.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;

    public void notify(String email, String type, String category, String title, String description) {
        Notification notification = Notification.builder()
                .type(type)
                .category(category)
                .title(title)
                .description(description)
                .targetEmail(email)
                .sentAt(LocalDateTime.now())
                .read(false)
                .build();

        repository.save(notification);
    }
}
