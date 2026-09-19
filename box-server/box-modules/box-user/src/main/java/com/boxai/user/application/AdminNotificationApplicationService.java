package com.boxai.user.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.conversation.MessageFeedbackRepository;
import com.boxai.domain.ops.OpsPlacement;
import com.boxai.domain.ops.OpsPlacementRepository;
import com.boxai.domain.platform.PlatformAdminInboxDismissRepository;
import com.boxai.domain.platform.PlatformAdminInboxReadRepository;
import com.boxai.security.context.LoginUser;
import com.boxai.security.context.SecurityContexts;
import com.boxai.security.permission.PlatformAdminAccess;
import com.boxai.user.api.AdminInboxItemVO;
import com.boxai.user.api.AdminInboxVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AdminNotificationApplicationService {

    private static final String KEY_FEEDBACK_PENDING = "feedback:pending";
    private static final String TYPE_TASK = "TASK";
    private static final String TYPE_ANNOUNCEMENT = "ANNOUNCEMENT";

    private final MessageFeedbackRepository messageFeedbackRepository;
    private final OpsPlacementRepository opsPlacementRepository;
    private final PlatformAdminInboxDismissRepository inboxDismissRepository;
    private final PlatformAdminInboxReadRepository inboxReadRepository;

    public AdminNotificationApplicationService(MessageFeedbackRepository messageFeedbackRepository,
                                                 OpsPlacementRepository opsPlacementRepository,
                                                 PlatformAdminInboxDismissRepository inboxDismissRepository,
                                                 PlatformAdminInboxReadRepository inboxReadRepository) {
        this.messageFeedbackRepository = messageFeedbackRepository;
        this.opsPlacementRepository = opsPlacementRepository;
        this.inboxDismissRepository = inboxDismissRepository;
        this.inboxReadRepository = inboxReadRepository;
    }

    public AdminInboxVO inbox() {
        LoginUser user = SecurityContexts.currentUser();
        Set<String> dismissed = inboxDismissRepository.findDismissedKeys(user.userId());
        Map<String, String> readMarkers = inboxReadRepository.findReadMarkers(user.userId());
        List<AdminInboxItemVO> items = collectItems(user, dismissed, readMarkers);
        long unread = items.stream().filter(item -> !item.read()).count();
        return new AdminInboxVO(unread, items);
    }

    @Transactional
    public void markRead(String key) {
        String normalized = normalizeKey(key);
        LoginUser user = SecurityContexts.currentUser();
        String marker = readMarkerForKey(normalized, user);
        inboxReadRepository.markRead(user.userId(), normalized, marker);
    }

    @Transactional
    public void markAllRead() {
        LoginUser user = SecurityContexts.currentUser();
        Set<String> dismissed = inboxDismissRepository.findDismissedKeys(user.userId());
        Map<String, String> readMarkers = inboxReadRepository.findReadMarkers(user.userId());
        for (AdminInboxItemVO item : collectItems(user, dismissed, readMarkers)) {
            if (item.read()) {
                continue;
            }
            inboxReadRepository.markRead(user.userId(), item.key(), readMarkerForKey(item.key(), user));
        }
    }

    @Transactional
    public void dismiss(String key) {
        String normalized = normalizeKey(key);
        if (KEY_FEEDBACK_PENDING.equals(normalized)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "待办通知不可关闭");
        }
        if (!normalized.startsWith("ops:")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "通知键无效");
        }
        LoginUser user = SecurityContexts.currentUser();
        inboxDismissRepository.dismiss(user.userId(), normalized);
    }

    private List<AdminInboxItemVO> collectItems(LoginUser user,
                                                Set<String> dismissed,
                                                Map<String, String> readMarkers) {
        List<AdminInboxItemVO> items = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        if (canSeeFeedbackTasks(user.platformAdminRole())) {
            long pending = messageFeedbackRepository.countByRatingAndStatus("BAD", "PENDING");
            if (pending > 0) {
                String body = pending == 1 ? "1 条踩反馈待回复" : pending + " 条踩反馈待回复";
                boolean read = !isFeedbackUnread(pending, readMarkers);
                items.add(new AdminInboxItemVO(
                        KEY_FEEDBACK_PENDING,
                        TYPE_TASK,
                        "待处理消息反馈",
                        body,
                        "/message-feedbacks?status=PENDING&rating=BAD",
                        "去处理",
                        "warning",
                        false,
                        read,
                        now));
            }
        }

        List<OpsPlacement> announcements = opsPlacementRepository.listActive(null, "B", now).stream()
                .filter(item -> "ADMIN_HEADER".equals(item.getSlot()))
                .sorted(Comparator.comparing(
                                OpsPlacement::getSortOrder,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(OpsPlacement::getId))
                .toList();

        for (OpsPlacement placement : announcements) {
            String noticeKey = opsKey(placement.getId());
            if (dismissed.contains(noticeKey)) {
                continue;
            }
            boolean read = readMarkers.containsKey(noticeKey);
            items.add(new AdminInboxItemVO(
                    noticeKey,
                    TYPE_ANNOUNCEMENT,
                    placement.getTitle(),
                    placement.getBody(),
                    placement.getLinkUrl(),
                    placement.getLinkLabel(),
                    placement.getTheme(),
                    placement.getDismissible() == null || placement.getDismissible() == 1,
                    read,
                    placement.getCreatedAt() == null ? now : placement.getCreatedAt()));
        }

        items.sort(Comparator
                .comparing((AdminInboxItemVO item) -> TYPE_TASK.equals(item.type()) ? 0 : 1)
                .thenComparing(AdminInboxItemVO::createdAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return items;
    }

    private String readMarkerForKey(String key, LoginUser user) {
        if (KEY_FEEDBACK_PENDING.equals(key)) {
            if (!canSeeFeedbackTasks(user.platformAdminRole())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "通知键无效");
            }
            long pending = messageFeedbackRepository.countByRatingAndStatus("BAD", "PENDING");
            if (pending <= 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "通知不存在");
            }
            return String.valueOf(pending);
        }
        if (key.startsWith("ops:")) {
            return null;
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "通知键无效");
    }

    private String normalizeKey(String key) {
        if (key == null || key.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "通知键无效");
        }
        return key.trim();
    }

    private boolean isFeedbackUnread(long pending, Map<String, String> readMarkers) {
        if (pending <= 0) {
            return false;
        }
        String marker = readMarkers.get(KEY_FEEDBACK_PENDING);
        if (marker == null || marker.isBlank()) {
            return true;
        }
        try {
            return pending > Long.parseLong(marker);
        } catch (NumberFormatException ex) {
            return true;
        }
    }

    private boolean canSeeFeedbackTasks(String platformAdminRole) {
        return PlatformAdminAccess.allows(platformAdminRole, "GET", "/api/v1/admin/message-feedbacks");
    }

    private static String opsKey(Long id) {
        return "ops:" + id;
    }
}
