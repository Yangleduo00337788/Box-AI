package com.boxai.user.api;

import java.util.List;

public record AdminInboxVO(
        long unreadCount,
        List<AdminInboxItemVO> items
) {
}
