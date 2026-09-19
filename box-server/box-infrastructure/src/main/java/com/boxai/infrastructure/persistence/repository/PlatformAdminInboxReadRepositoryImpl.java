package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.platform.PlatformAdminInboxReadRepository;
import com.boxai.infrastructure.persistence.entity.PlatformAdminInboxReadDO;
import com.boxai.infrastructure.persistence.mapper.PlatformAdminInboxReadMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Repository
public class PlatformAdminInboxReadRepositoryImpl implements PlatformAdminInboxReadRepository {

    private final PlatformAdminInboxReadMapper mapper;

    public PlatformAdminInboxReadRepositoryImpl(PlatformAdminInboxReadMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Map<String, String> findReadMarkers(long adminUserId) {
        Map<String, String> markers = new HashMap<>();
        mapper.selectListByQuery(QueryWrapper.create().eq("admin_user_id", adminUserId))
                .forEach(row -> markers.put(row.getNoticeKey(), row.getReadMarker()));
        return markers;
    }

    @Override
    public void markRead(long adminUserId, String noticeKey, String readMarker) {
        PlatformAdminInboxReadDO existing = mapper.selectOneByQuery(QueryWrapper.create()
                .eq("admin_user_id", adminUserId)
                .eq("notice_key", noticeKey));
        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            UpdateChain.of(PlatformAdminInboxReadDO.class)
                    .set("read_marker", readMarker)
                    .set("read_at", now)
                    .where("id = ?", existing.getId())
                    .update();
            return;
        }
        PlatformAdminInboxReadDO row = new PlatformAdminInboxReadDO();
        row.setAdminUserId(adminUserId);
        row.setNoticeKey(noticeKey);
        row.setReadMarker(readMarker);
        row.setReadAt(now);
        mapper.insert(row);
    }
}
