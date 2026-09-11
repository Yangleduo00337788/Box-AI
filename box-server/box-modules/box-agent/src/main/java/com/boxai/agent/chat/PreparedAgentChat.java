package com.boxai.agent.chat;

import com.boxai.ai.ChatTurn;
import com.boxai.ai.ModelRuntimeConfig;
import com.boxai.ai.ToolDefinition;

import java.util.List;

public record PreparedAgentChat(
        ModelRuntimeConfig runtimeConfig,
        List<ChatTurn> turns,
        Double temperature,
        Double topP,
        Integer maxTokens,
        Long credentialId,
        boolean platformCredential,
        Long modelId,
        Long agentVersionId,
        List<ToolDefinition> tools
) {
}
