package com.boxai.user.api;

import java.util.List;

public record SidebarPinsVO(
        List<Long> pinnedAgentIds,
        List<Long> pinnedConversationIds
) {}
