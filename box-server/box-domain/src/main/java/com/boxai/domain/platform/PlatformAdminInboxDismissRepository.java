package com.boxai.domain.platform;

import java.util.Set;

public interface PlatformAdminInboxDismissRepository {

    Set<String> findDismissedKeys(long adminUserId);

    void dismiss(long adminUserId, String noticeKey);
}
