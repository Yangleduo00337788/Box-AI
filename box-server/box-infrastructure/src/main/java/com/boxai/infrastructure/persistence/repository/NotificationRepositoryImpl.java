package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.notification.Notification;
import com.boxai.domain.notification.NotificationRepository;
import com.boxai.infrastructure.persistence.entity.NotificationDO;
import com.boxai.infrastructure.persistence.mapper.NotificationMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class NotificationRepositoryImpl implements NotificationRepository {

    private final NotificationMapper mapper;

    public NotificationRepositoryImpl(NotificationMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Notification save(Notification notification) {
        NotificationDO row = toDo(notification);
        row.setCreatedAt(LocalDateTime.now());
        mapper.insert(row);
        notification.setId(row.getId());
        notification.setCreatedAt(row.getCreatedAt());
        return notification;
    }

    @Override
    public void markRead(Long id, Long userId, Long workspaceId) {
        NotificationDO row = mapper.selectOneById(id);
        if (row == null || !row.getUserId().equals(userId) || !row.getWorkspaceId().equals(workspaceId)) {
            return;
        }
        row.setReadFlag(1);
        mapper.update(row);
    }

    @Override
    public void markAllRead(Long userId, Long workspaceId) {
        List<NotificationDO> rows = mapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("user_id", userId)
                        .eq("workspace_id", workspaceId)
                        .eq("read_flag", 0));
        for (NotificationDO row : rows) {
            row.setReadFlag(1);
            mapper.update(row);
        }
    }

    @Override
    public int countUnread(Long userId, Long workspaceId) {
        Long count = mapper.selectCountByQuery(
                QueryWrapper.create()
                        .eq("user_id", userId)
                        .eq("workspace_id", workspaceId)
                        .eq("read_flag", 0));
        return count == null ? 0 : count.intValue();
    }

    @Override
    public List<Notification> listByUser(Long userId, Long workspaceId, int limit) {
        return mapper.selectListByQuery(
                        QueryWrapper.create()
                                .eq("user_id", userId)
                                .eq("workspace_id", workspaceId)
                                .orderBy("created_at", false)
                                .limit(Math.max(limit, 1)))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Notification> findById(Long id) {
        return Optional.ofNullable(mapper.selectOneById(id)).map(this::toDomain);
    }

    private Notification toDomain(NotificationDO row) {
        Notification notification = new Notification();
        notification.setId(row.getId());
        notification.setWorkspaceId(row.getWorkspaceId());
        notification.setUserId(row.getUserId());
        notification.setTitle(row.getTitle());
        notification.setContent(row.getContent());
        notification.setCategory(row.getCategory());
        notification.setLinkUrl(row.getLinkUrl());
        notification.setRead(row.getReadFlag() != null && row.getReadFlag() == 1);
        notification.setCreatedAt(row.getCreatedAt());
        return notification;
    }

    private NotificationDO toDo(Notification notification) {
        NotificationDO row = new NotificationDO();
        row.setWorkspaceId(notification.getWorkspaceId());
        row.setUserId(notification.getUserId());
        row.setTitle(notification.getTitle());
        row.setContent(notification.getContent());
        row.setCategory(notification.getCategory());
        row.setLinkUrl(notification.getLinkUrl());
        row.setReadFlag(Boolean.TRUE.equals(notification.getRead()) ? 1 : 0);
        return row;
    }
}
