package com.boxai.conversation.api;

public record SendMessageVO(
        MessageVO userMessage,
        MessageVO assistantMessage
) {
}
