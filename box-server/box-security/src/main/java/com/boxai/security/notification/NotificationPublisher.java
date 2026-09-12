package com.boxai.security.notification;

import com.boxai.domain.notification.Notification;
import com.boxai.domain.notification.NotificationRepository;
import org.springframework.stereotype.Service;

@Service
public class NotificationPublisher {

    private final NotificationRepository notificationRepository;

    public NotificationPublisher(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void publish(Long userId,
                        Long workspaceId,
                        String title,
                        String content,
                        String category,
                        String linkUrl) {
        if (userId == null || workspaceId == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setWorkspaceId(workspaceId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setCategory(category == null || category.isBlank() ? "SYSTEM" : category);
        notification.setLinkUrl(linkUrl);
        notification.setRead(false);
        notificationRepository.save(notification);
    }
}
