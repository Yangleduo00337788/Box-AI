package com.boxai.infrastructure.ai;

import com.boxai.ai.ChatModelGateway;
import com.boxai.ai.ChatStreamHandler;
import com.boxai.ai.ChatTurn;
import com.boxai.ai.ModelRuntimeConfig;
import com.boxai.ai.ToolDefinition;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class OpenAiCompatibleChatModelGateway implements ChatModelGateway {

    @Override
    public String chat(ModelRuntimeConfig config, String userMessage) {
        return chat(config, null, userMessage, null, null, null);
    }

    @Override
    public String chat(ModelRuntimeConfig config,
                       String systemPrompt,
                       String userMessage,
                       Double temperature,
                       Double topP,
                       Integer maxTokens) {
        List<ChatTurn> turns = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            turns.add(new ChatTurn("SYSTEM", systemPrompt));
        }
        turns.add(new ChatTurn("USER", userMessage));
        return chat(config, turns, temperature, topP, maxTokens);
    }

    @Override
    public String chat(ModelRuntimeConfig config,
                       List<ChatTurn> turns,
                       Double temperature,
                       Double topP,
                       Integer maxTokens) {
        ChatModel model = buildChatModel(config, temperature, topP, maxTokens);
        return model.chat(toMessages(turns)).aiMessage().text();
    }

    private static final Pattern TOOL_CALL_PATTERN = Pattern.compile("\\{\\s*\"tool\"\\s*:\\s*\"([^\"]+)\"\\s*\\}");

    @Override
    public String chatWithTools(ModelRuntimeConfig config,
                                List<ChatTurn> turns,
                                List<ToolDefinition> tools,
                                Function<String, String> toolExecutor,
                                Double temperature,
                                Double topP,
                                Integer maxTokens) {
        if (tools == null || tools.isEmpty()) {
            return chat(config, turns, temperature, topP, maxTokens);
        }
        List<ChatTurn> workingTurns = new ArrayList<>(turns == null ? List.of() : turns);
        prependToolInstruction(workingTurns, tools);
        for (int round = 0; round < 5; round++) {
            String response = chat(config, workingTurns, temperature, topP, maxTokens);
            Matcher matcher = TOOL_CALL_PATTERN.matcher(response);
            if (!matcher.find()) {
                return response;
            }
            String toolKey = matcher.group(1);
            String toolResult = toolExecutor.apply(toolKey);
            workingTurns.add(new ChatTurn("ASSISTANT", response));
            workingTurns.add(new ChatTurn("USER", "工具 " + toolKey + " 的执行结果：\n" + toolResult + "\n请基于结果继续回答用户。"));
        }
        throw new BusinessException(ErrorCode.EXECUTION_FAILED, "工具调用超过最大轮次");
    }

    private void prependToolInstruction(List<ChatTurn> turns, List<ToolDefinition> tools) {
        StringBuilder builder = new StringBuilder("你可以调用以下工具。需要调用时，仅回复 JSON：{\"tool\":\"tool_key\"}\n");
        for (ToolDefinition tool : tools) {
            builder.append("- ").append(tool.name());
            if (tool.description() != null && !tool.description().isBlank()) {
                builder.append(": ").append(tool.description());
            }
            builder.append('\n');
        }
        if (!turns.isEmpty() && "SYSTEM".equalsIgnoreCase(turns.get(0).role())) {
            ChatTurn first = turns.get(0);
            turns.set(0, new ChatTurn("SYSTEM", first.content() + "\n\n" + builder));
        } else {
            turns.add(0, new ChatTurn("SYSTEM", builder.toString()));
        }
    }

    @Override
    public void streamChat(ModelRuntimeConfig config,
                           String systemPrompt,
                           String userMessage,
                           Double temperature,
                           Double topP,
                           Integer maxTokens,
                           ChatStreamHandler handler) {
        List<ChatTurn> turns = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            turns.add(new ChatTurn("SYSTEM", systemPrompt));
        }
        turns.add(new ChatTurn("USER", userMessage));
        streamChat(config, turns, temperature, topP, maxTokens, handler);
    }

    @Override
    public void streamChat(ModelRuntimeConfig config,
                           List<ChatTurn> turns,
                           Double temperature,
                           Double topP,
                           Integer maxTokens,
                           ChatStreamHandler handler) {
        StreamingChatModel model = buildStreamingModel(config, temperature, topP, maxTokens);
        model.chat(toMessages(turns), new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                handler.onPartial(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                handler.onComplete();
            }

            @Override
            public void onError(Throwable error) {
                handler.onError(error);
            }
        });
    }

    private List<ChatMessage> toMessages(List<ChatTurn> turns) {
        List<ChatMessage> messages = new ArrayList<>();
        for (ChatTurn turn : turns) {
            if (turn.content() == null || turn.content().isBlank()) {
                continue;
            }
            String role = turn.role() == null ? "" : turn.role().toUpperCase();
            switch (role) {
                case "SYSTEM" -> messages.add(SystemMessage.from(turn.content()));
                case "USER" -> messages.add(UserMessage.from(turn.content()));
                case "ASSISTANT" -> messages.add(AiMessage.from(turn.content()));
                default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的消息角色: " + turn.role());
            }
        }
        if (messages.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "消息内容不能为空");
        }
        return messages;
    }

    private ChatModel buildChatModel(ModelRuntimeConfig config, Double temperature, Double topP, Integer maxTokens) {
        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .apiKey(config.apiKey())
                .baseUrl(normalizeBaseUrl(config.baseUrl()))
                .modelName(config.modelName())
                .timeout(Duration.ofSeconds(60))
                .logRequests(false)
                .logResponses(false);
        applyGenerationParams(builder, temperature, topP, maxTokens);
        return builder.build();
    }

    private StreamingChatModel buildStreamingModel(ModelRuntimeConfig config,
                                                   Double temperature,
                                                   Double topP,
                                                   Integer maxTokens) {
        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
                .apiKey(config.apiKey())
                .baseUrl(normalizeBaseUrl(config.baseUrl()))
                .modelName(config.modelName())
                .timeout(Duration.ofSeconds(60))
                .logRequests(false)
                .logResponses(false);
        applyGenerationParams(builder, temperature, topP, maxTokens);
        return builder.build();
    }

    private void applyGenerationParams(OpenAiChatModel.OpenAiChatModelBuilder builder,
                                       Double temperature,
                                       Double topP,
                                       Integer maxTokens) {
        if (temperature != null) {
            builder.temperature(temperature);
        }
        if (topP != null) {
            builder.topP(topP);
        }
        if (maxTokens != null) {
            builder.maxTokens(maxTokens);
        }
    }

    private void applyGenerationParams(OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder,
                                       Double temperature,
                                       Double topP,
                                       Integer maxTokens) {
        if (temperature != null) {
            builder.temperature(temperature);
        }
        if (topP != null) {
            builder.topP(topP);
        }
        if (maxTokens != null) {
            builder.maxTokens(maxTokens);
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://api.openai.com/v1";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
