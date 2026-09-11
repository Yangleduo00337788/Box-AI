package com.boxai.agent.api;

public record ChatStreamEvent(String type, String content, String message) {

    public static ChatStreamEvent delta(String content) {
        return new ChatStreamEvent("delta", content, null);
    }

    public static ChatStreamEvent done() {
        return new ChatStreamEvent("done", null, null);
    }

    public static ChatStreamEvent error(String message) {
        return new ChatStreamEvent("error", null, message);
    }
}
