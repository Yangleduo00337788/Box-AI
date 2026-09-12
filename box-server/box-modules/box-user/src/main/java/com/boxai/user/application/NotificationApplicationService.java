package com.boxai.user.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.notification.Notification;
import com.boxai.domain.notification.NotificationRepository;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.user.api.NotificationVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationApplicationService {

    private final NotificationRepository notificationRepository;

    public NotificationApplicationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<NotificationVO> list(int limit) {
        Long userId = WorkspaceContext.require().userId();
        Long workspaceId = WorkspaceContext.require().workspaceId();
        int safeLimit = limit <= 0 ? 20 : Math.min(limit, 100);
        return notificationRepository.listByUser(userId, workspaceId, safeLimit).stream().map(this::toVO).toList();
    }

    public int unreadCount() {
        Long userId = WorkspaceContext.require().userId();
        Long workspaceId = WorkspaceContext.require().workspaceId();
        return notificationRepository.countUnread(userId, workspaceId);
    }

    @Transactional
    public void markRead(Long id) {
        Long userId = WorkspaceContext.require().userId();
        Long workspaceId = WorkspaceContext.require().workspaceId();
        notificationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "通知不存在"));
        notificationRepository.markRead(id, userId, workspaceId);
    }

    @Transactional
    public void markAllRead() {
        Long userId = WorkspaceContext.require().userId();
        Long workspaceId = WorkspaceContext.require().workspaceId();
        notificationRepository.markAllRead(userId, workspaceId);
    }

    @Transactional
    public NotificationVO create(Long userId, Long workspaceId, String title, String content, String category, String linkUrl) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setWorkspaceId(workspaceId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setCategory(category == null ? "SYSTEM" : category);
        notification.setLinkUrl(linkUrl);
        notification.setRead(false);
        notificationRepository.save(notification);
        return toVO(notification);
    }

    private NotificationVO toVO(Notification notification) {
        return new NotificationVO(
                notification.getId(),
                notification.getTitle(),
                notification.getContent(),
                notification.getCategory(),
                notification.getLinkUrl(),
                notification.getRead(),
                notification.getCreatedAt());
    }
}
