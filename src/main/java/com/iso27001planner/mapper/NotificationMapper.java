package com.iso27001planner.mapper;

import com.iso27001planner.dto.NotificationDTO;
import com.iso27001planner.entity.Notification;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NotificationMapper {

    public NotificationDTO toDTO(Notification notification) {
        return new NotificationDTO(
                String.valueOf(notification.getId()),
                notification.getType(),
                notification.getTitle(),
                notification.getDescription(),
                notification.getSentAt().toString(),
                notification.isRead(),
                notification.getCategory()
        );
    }

    public List<NotificationDTO> toDTOList(List<Notification> list) {
        return list.stream().map(this::toDTO).toList();
    }
}
