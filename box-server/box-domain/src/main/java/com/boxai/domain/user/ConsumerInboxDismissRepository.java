package com.boxai.domain.user;

import java.util.Set;

public interface ConsumerInboxDismissRepository {

    Set<String> findDismissedKeys(long userId, long workspaceId);

    void dismiss(long userId, long workspaceId, String noticeKey);
}
