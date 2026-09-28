package com.boxai.user.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.notification.Notification;
import com.boxai.domain.notification.NotificationRepository;
import com.boxai.domain.ops.OpsPlacement;
import com.boxai.domain.ops.OpsPlacementRepository;
import com.boxai.domain.user.ConsumerInboxDismissRepository;
import com.boxai.domain.user.ConsumerInboxReadRepository;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.user.api.NotificationVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class NotificationApplicationService {

    private static final String OPS_KEY_PREFIX = "ops:";
    private static final String SLOT_CONSUMER_INBOX = "CONSUMER_INBOX";

    private final NotificationRepository notificationRepository;
    private final OpsPlacementRepository opsPlacementRepository;
    private final ConsumerInboxReadRepository consumerInboxReadRepository;
    private final ConsumerInboxDismissRepository consumerInboxDismissRepository;

    public NotificationApplicationService(NotificationRepository notificationRepository,
                                          OpsPlacementRepository opsPlacementRepository,
                                          ConsumerInboxReadRepository consumerInboxReadRepository,
                                          ConsumerInboxDismissRepository consumerInboxDismissRepository) {
        this.notificationRepository = notificationRepository;
        this.opsPlacementRepository = opsPlacementRepository;
        this.consumerInboxReadRepository = consumerInboxReadRepository;
        this.consumerInboxDismissRepository = consumerInboxDismissRepository;
    }

    public List<NotificationVO> list(int limit) {
        Long userId = WorkspaceContext.require().userId();
        Long workspaceId = WorkspaceContext.require().workspaceId();
        int safeLimit = limit <= 0 ? 20 : Math.min(limit, 100);
        List<NotificationVO> items = new ArrayList<>();
        items.addAll(listOpsAnnouncements(userId, workspaceId));
        notificationRepository.listByUser(userId, workspaceId, safeLimit).stream()
                .map(this::toVO)
                .forEach(items::add);
        items.sort(Comparator.comparing(NotificationVO::createdAt, Comparator.nullsLast(Comparator.reverseOrder())));
        if (items.size() > safeLimit) {
            return items.subList(0, safeLimit);
        }
        return items;
    }

    public int unreadCount() {
        Long userId = WorkspaceContext.require().userId();
        Long workspaceId = WorkspaceContext.require().workspaceId();
        int count = notificationRepository.countUnread(userId, workspaceId);
        Set<String> readKeys = consumerInboxReadRepository.findReadKeys(userId, workspaceId);
        Set<String> dismissed = consumerInboxDismissRepository.findDismissedKeys(userId, workspaceId);
        LocalDateTime now = LocalDateTime.now();
        for (OpsPlacement placement : opsPlacementRepository.listActive(SLOT_CONSUMER_INBOX, "C", now)) {
            String key = opsKey(placement.getId());
            if (!dismissed.contains(key) && !readKeys.contains(key)) {
                count++;
            }
        }
        return count;
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
    public void markReadByKey(String key) {
        String normalized = normalizeOpsKey(key);
        Long userId = WorkspaceContext.require().userId();
        Long workspaceId = WorkspaceContext.require().workspaceId();
        requireActiveOpsPlacement(normalized);
        consumerInboxReadRepository.markRead(userId, workspaceId, normalized);
    }

    @Transactional
    public void dismissByKey(String key) {
        String normalized = normalizeOpsKey(key);
        Long userId = WorkspaceContext.require().userId();
        Long workspaceId = WorkspaceContext.require().workspaceId();
        OpsPlacement placement = requireActiveOpsPlacement(normalized);
        if (placement.getDismissible() == null || placement.getDismissible() != 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该公告不可关闭");
        }
        consumerInboxDismissRepository.dismiss(userId, workspaceId, normalized);
        consumerInboxReadRepository.markRead(userId, workspaceId, normalized);
    }

    @Transactional
    public void markAllRead() {
        Long userId = WorkspaceContext.require().userId();
        Long workspaceId = WorkspaceContext.require().workspaceId();
        notificationRepository.markAllRead(userId, workspaceId);
        LocalDateTime now = LocalDateTime.now();
        for (OpsPlacement placement : opsPlacementRepository.listActive(SLOT_CONSUMER_INBOX, "C", now)) {
            String noticeKey = opsKey(placement.getId());
            if (!consumerInboxDismissRepository.findDismissedKeys(userId, workspaceId).contains(noticeKey)) {
                consumerInboxReadRepository.markRead(userId, workspaceId, noticeKey);
            }
        }
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

    private List<NotificationVO> listOpsAnnouncements(long userId, long workspaceId) {
        Set<String> readKeys = consumerInboxReadRepository.findReadKeys(userId, workspaceId);
        Set<String> dismissed = consumerInboxDismissRepository.findDismissedKeys(userId, workspaceId);
        LocalDateTime now = LocalDateTime.now();
        List<NotificationVO> items = new ArrayList<>();
        for (OpsPlacement placement : opsPlacementRepository.listActive(SLOT_CONSUMER_INBOX, "C", now)) {
            String noticeKey = opsKey(placement.getId());
            if (dismissed.contains(noticeKey)) {
                continue;
            }
            boolean read = readKeys.contains(noticeKey);
            items.add(new NotificationVO(
                    null,
                    noticeKey,
                    placement.getTitle(),
                    placement.getBody(),
                    "OPS_ANNOUNCEMENT",
                    placement.getLinkUrl(),
                    placement.getLinkLabel(),
                    placement.getDismissible() == null || placement.getDismissible() == 1,
                    read,
                    placement.getCreatedAt() == null ? now : placement.getCreatedAt()));
        }
        return items;
    }

    private OpsPlacement requireActiveOpsPlacement(String key) {
        long id = parseOpsId(key);
        OpsPlacement placement = opsPlacementRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "公告不存在"));
        if (!SLOT_CONSUMER_INBOX.equals(placement.getSlot()) || !"C".equals(placement.getAudience())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "通知键无效");
        }
        if (!"LISTED".equals(placement.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "公告不存在");
        }
        return placement;
    }

    private String normalizeOpsKey(String key) {
        if (key == null || key.isBlank() || !key.startsWith(OPS_KEY_PREFIX)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "通知键无效");
        }
        return key.trim();
    }

    private static long parseOpsId(String key) {
        try {
            return Long.parseLong(key.substring(OPS_KEY_PREFIX.length()));
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "通知键无效");
        }
    }

    private static String opsKey(Long id) {
        return OPS_KEY_PREFIX + id;
    }

    private NotificationVO toVO(Notification notification) {
        return new NotificationVO(
                notification.getId(),
                null,
                notification.getTitle(),
                notification.getContent(),
                notification.getCategory(),
                notification.getLinkUrl(),
                null,
                false,
                notification.getRead(),
                notification.getCreatedAt());
    }
}
