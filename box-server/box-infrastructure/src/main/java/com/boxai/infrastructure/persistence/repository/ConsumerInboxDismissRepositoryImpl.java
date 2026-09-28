package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.user.ConsumerInboxDismissRepository;
import com.boxai.infrastructure.persistence.entity.ConsumerInboxDismissDO;
import com.boxai.infrastructure.persistence.mapper.ConsumerInboxDismissMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Repository
public class ConsumerInboxDismissRepositoryImpl implements ConsumerInboxDismissRepository {

    private final ConsumerInboxDismissMapper mapper;

    public ConsumerInboxDismissRepositoryImpl(ConsumerInboxDismissMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Set<String> findDismissedKeys(long userId, long workspaceId) {
        Set<String> keys = new HashSet<>();
        mapper.selectListByQuery(QueryWrapper.create()
                        .eq("user_id", userId)
                        .eq("workspace_id", workspaceId))
                .forEach(row -> keys.add(row.getNoticeKey()));
        return keys;
    }

    @Override
    public void dismiss(long userId, long workspaceId, String noticeKey) {
        ConsumerInboxDismissDO existing = mapper.selectOneByQuery(QueryWrapper.create()
                .eq("user_id", userId)
                .eq("workspace_id", workspaceId)
                .eq("notice_key", noticeKey));
        if (existing != null) {
            return;
        }
        ConsumerInboxDismissDO row = new ConsumerInboxDismissDO();
        row.setUserId(userId);
        row.setWorkspaceId(workspaceId);
        row.setNoticeKey(noticeKey);
        row.setCreatedAt(LocalDateTime.now());
        mapper.insert(row);
    }
}
