package com.boxai.domain.platform;

import java.util.Map;

public interface PlatformAdminInboxReadRepository {

    /** notice_key -> read_marker（可为 null） */
    Map<String, String> findReadMarkers(long adminUserId);

    void markRead(long adminUserId, String noticeKey, String readMarker);
}
