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

    public static ChatStreamEvent queued(int position) {
        String hint = position <= 0
                ? "当前对话火爆，正在排队"
                : "排队中，前面还有 " + position + " 人";
        return new ChatStreamEvent("queued", String.valueOf(Math.max(position, 1)), hint, null);
    }

    public static ChatStreamEvent toolStart(String payload) {
        return new ChatStreamEvent("tool.start", payload, null, null);
    }

    public static ChatStreamEvent toolDelta(String payload) {
        return new ChatStreamEvent("tool.delta", payload, null, null);
    }

    public static ChatStreamEvent toolEnd(String payload) {
        return new ChatStreamEvent("tool.end", payload, null, null);
    }

    public static ChatStreamEvent toolConfirmRequired(String payload) {
        return new ChatStreamEvent("tool.confirm", payload, null, null);
    }
}
