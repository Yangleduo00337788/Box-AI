package com.boxai.domain.user;

import java.util.Set;

public interface ConsumerInboxReadRepository {

    Set<String> findReadKeys(long userId, long workspaceId);

    void markRead(long userId, long workspaceId, String noticeKey);
}
