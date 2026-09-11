package com.boxai.ai;

public interface LlmProvider {

    String providerName();

    boolean available();
}
