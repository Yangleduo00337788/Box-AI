package com.boxai.infrastructure.ai;

import com.boxai.ai.LlmProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OpenAiCompatibleLlmProvider implements LlmProvider {

    private final String apiKey;

    public OpenAiCompatibleLlmProvider(@Value("${box.ai.openai.api-key:}") String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String providerName() {
        return "openai-compatible";
    }

    @Override
    public boolean available() {
        return apiKey != null && !apiKey.isBlank();
    }
}
