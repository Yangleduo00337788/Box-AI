package com.boxai.agent.chat;

import com.boxai.ai.ChatTurn;
import com.boxai.ai.ModelRuntimeConfig;
import com.boxai.ai.ToolDefinition;

import java.util.List;

public record PreparedAgentChat(
        Long agentId,
        ModelRuntimeConfig runtimeConfig,
        List<ChatTurn> turns,
        Double temperature,
        Double topP,
        Integer maxTokens,
        Long credentialId,
        boolean platformCredential,
        Long modelId,
        Long agentVersionId,
        List<ToolDefinition> tools,
        String toolConfirmationToken
) {
    public PreparedAgentChat withToolConfirmationToken(String token) {
        return new PreparedAgentChat(
                agentId,
                runtimeConfig,
                turns,
                temperature,
                topP,
                maxTokens,
                credentialId,
                platformCredential,
                modelId,
                agentVersionId,
                tools,
                token);
    }
}
