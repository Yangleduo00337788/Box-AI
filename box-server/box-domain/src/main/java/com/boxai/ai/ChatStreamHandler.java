package com.boxai.ai;

public interface ChatStreamHandler {

    void onPartial(String partial);

    void onComplete();

    void onError(Throwable error);
}
