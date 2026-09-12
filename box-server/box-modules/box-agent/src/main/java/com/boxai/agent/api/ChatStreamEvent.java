package com.boxai.agent.api;

public record ChatStreamEvent(String type, String content, String message, Long executionId) {

    public ChatStreamEvent(String type, String content, String message) {
        this(type, content, message, null);
    }

    public static ChatStreamEvent delta(String content) {
        return new ChatStreamEvent("delta", content, null, null);
    }

    public static ChatStreamEvent citations(String citationsJson) {
        return new ChatStreamEvent("citations", citationsJson, null, null);
    }

    public static ChatStreamEvent done() {
        return new ChatStreamEvent("done", null, null, null);
    }

    public static ChatStreamEvent done(Long executionId) {
        return new ChatStreamEvent("done", null, null, executionId);
    }

    public static ChatStreamEvent error(String message) {
        return new ChatStreamEvent("error", null, message, null);
    }
}
