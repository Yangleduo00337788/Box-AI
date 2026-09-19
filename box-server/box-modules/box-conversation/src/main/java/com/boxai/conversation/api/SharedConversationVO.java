package com.boxai.conversation.api;

import java.time.LocalDateTime;
import java.util.List;

public record SharedConversationVO(
        String title,
        LocalDateTime sharedAt,
        String userName,
        String agentName,
        List<SharedMessageVO> messages
) {
}
