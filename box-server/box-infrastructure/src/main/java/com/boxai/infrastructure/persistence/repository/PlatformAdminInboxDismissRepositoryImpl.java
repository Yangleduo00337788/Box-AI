package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.platform.PlatformAdminInboxDismissRepository;
import com.boxai.infrastructure.persistence.entity.PlatformAdminInboxDismissDO;
import com.boxai.infrastructure.persistence.mapper.PlatformAdminInboxDismissMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class PlatformAdminInboxDismissRepositoryImpl implements PlatformAdminInboxDismissRepository {

    private final PlatformAdminInboxDismissMapper mapper;

    public PlatformAdminInboxDismissRepositoryImpl(PlatformAdminInboxDismissMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Set<String> findDismissedKeys(long adminUserId) {
        return mapper.selectListByQuery(QueryWrapper.create().eq("admin_user_id", adminUserId))
                .stream()
                .map(PlatformAdminInboxDismissDO::getNoticeKey)
                .collect(Collectors.toSet());
    }

    @Override
    public void dismiss(long adminUserId, String noticeKey) {
        PlatformAdminInboxDismissDO existing = mapper.selectOneByQuery(QueryWrapper.create()
                .eq("admin_user_id", adminUserId)
                .eq("notice_key", noticeKey));
        if (existing != null) {
            return;
        }
        PlatformAdminInboxDismissDO row = new PlatformAdminInboxDismissDO();
        row.setAdminUserId(adminUserId);
        row.setNoticeKey(noticeKey);
        row.setCreatedAt(LocalDateTime.now());
        mapper.insert(row);
    }
}
