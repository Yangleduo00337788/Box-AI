package com.boxai.ai;

@FunctionalInterface
public interface ToolStreamObserver {

    void onToolRound(ToolCall toolCall, String result);
}
