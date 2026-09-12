package com.boxai.domain.notification;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository {

    Notification save(Notification notification);

    void markRead(Long id, Long userId, Long workspaceId);

    void markAllRead(Long userId, Long workspaceId);

    int countUnread(Long userId, Long workspaceId);

    List<Notification> listByUser(Long userId, Long workspaceId, int limit);

    Optional<Notification> findById(Long id);
}
