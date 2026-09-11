package com.boxai.user.api;

import java.util.List;

public record ReplaceSidebarPinsRequest(
        List<Long> pinnedAgentIds,
        List<Long> pinnedConversationIds
) {}
