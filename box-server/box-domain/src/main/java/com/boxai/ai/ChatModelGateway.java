package com.boxai.ai;

import java.util.List;
import java.util.function.Function;

public interface ChatModelGateway {

    String chat(ModelRuntimeConfig config, String userMessage);

    String chat(ModelRuntimeConfig config,
                String systemPrompt,
                String userMessage,
                Double temperature,
                Double topP,
                Integer maxTokens);

    String chat(ModelRuntimeConfig config,
                List<ChatTurn> turns,
                Double temperature,
                Double topP,
                Integer maxTokens);

    String chatWithTools(ModelRuntimeConfig config,
                         List<ChatTurn> turns,
                         List<ToolDefinition> tools,
                         Function<String, String> toolExecutor,
                         Double temperature,
                         Double topP,
                         Integer maxTokens);

    void streamChat(ModelRuntimeConfig config,
                    String systemPrompt,
                    String userMessage,
                    Double temperature,
                    Double topP,
                    Integer maxTokens,
                    ChatStreamHandler handler);

    void streamChat(ModelRuntimeConfig config,
                    List<ChatTurn> turns,
                    Double temperature,
                    Double topP,
                    Integer maxTokens,
                    ChatStreamHandler handler);
}
