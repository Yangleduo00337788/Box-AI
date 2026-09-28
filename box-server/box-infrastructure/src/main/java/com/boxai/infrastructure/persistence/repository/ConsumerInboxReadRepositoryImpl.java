package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.user.ConsumerInboxReadRepository;
import com.boxai.infrastructure.persistence.entity.ConsumerInboxReadDO;
import com.boxai.infrastructure.persistence.mapper.ConsumerInboxReadMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Repository
public class ConsumerInboxReadRepositoryImpl implements ConsumerInboxReadRepository {

    private final ConsumerInboxReadMapper mapper;

    public ConsumerInboxReadRepositoryImpl(ConsumerInboxReadMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Set<String> findReadKeys(long userId, long workspaceId) {
        Set<String> keys = new HashSet<>();
        mapper.selectListByQuery(QueryWrapper.create()
                        .eq("user_id", userId)
                        .eq("workspace_id", workspaceId))
                .forEach(row -> keys.add(row.getNoticeKey()));
        return keys;
    }

    @Override
    public void markRead(long userId, long workspaceId, String noticeKey) {
        ConsumerInboxReadDO existing = mapper.selectOneByQuery(QueryWrapper.create()
                .eq("user_id", userId)
                .eq("workspace_id", workspaceId)
                .eq("notice_key", noticeKey));
        if (existing != null) {
            return;
        }
        ConsumerInboxReadDO row = new ConsumerInboxReadDO();
        row.setUserId(userId);
        row.setWorkspaceId(workspaceId);
        row.setNoticeKey(noticeKey);
        row.setCreatedAt(LocalDateTime.now());
        mapper.insert(row);
    }
}
