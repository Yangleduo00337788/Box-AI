package com.boxai.infrastructure.ai;

import com.boxai.ai.ChatModelGateway;
import com.boxai.ai.ChatStreamHandler;
import com.boxai.ai.ChatTurn;
import com.boxai.ai.ModelRuntimeConfig;
import com.boxai.ai.ToolCall;
import com.boxai.ai.ToolDefinition;
import com.boxai.ai.ToolStreamObserver;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class OpenAiCompatibleChatModelGateway implements ChatModelGateway {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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

    private static final Pattern TOOL_CALL_PATTERN = Pattern.compile("\\{[^{}]*\"tool\"\\s*:\\s*\"([^\"]+)\"[^{}]*\\}");

    @Override
    public String chatWithTools(ModelRuntimeConfig config,
                                List<ChatTurn> turns,
                                List<ToolDefinition> tools,
                                Function<ToolCall, String> toolExecutor,
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
            ToolCall toolCall = parseToolCall(response);
            if (toolCall == null) {
                return response;
            }
            String toolResult = toolExecutor.apply(toolCall);
            workingTurns.add(new ChatTurn("ASSISTANT", response));
            workingTurns.add(new ChatTurn("USER", "工具 " + toolCall.toolKey() + " 的执行结果：\n" + toolResult + "\n请基于结果继续回答用户。"));
        }
        throw new BusinessException(ErrorCode.EXECUTION_FAILED, "工具调用超过最大轮次");
    }

    private ToolCall parseToolCall(String response) {
        if (response == null || response.isBlank()) {
            return null;
        }
        Matcher matcher = TOOL_CALL_PATTERN.matcher(response);
        if (!matcher.find()) {
            return null;
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(matcher.group());
            String toolKey = node.path("tool").asText(null);
            if (toolKey == null || toolKey.isBlank()) {
                return null;
            }
            Map<String, Object> arguments = new LinkedHashMap<>();
            JsonNode argsNode = node.get("arguments");
            if (argsNode != null && argsNode.isObject()) {
                argsNode.fields().forEachRemaining(entry -> arguments.put(entry.getKey(), jsonValue(entry.getValue())));
            }
            return new ToolCall(toolKey, arguments);
        } catch (Exception ex) {
            return new ToolCall(matcher.group(1), Map.of());
        }
    }

    private Object jsonValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isNumber()) {
            return node.numberValue();
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        return node.toString();
    }

    private void prependToolInstruction(List<ChatTurn> turns, List<ToolDefinition> tools) {
        StringBuilder builder = new StringBuilder(
                "你可以调用以下工具。需要调用时，仅回复 JSON：{\"tool\":\"tool_key\",\"arguments\":{...}}\n");
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

    @Override
    public void streamChatWithTools(ModelRuntimeConfig config,
                                    List<ChatTurn> turns,
                                    List<ToolDefinition> tools,
                                    Function<ToolCall, String> toolExecutor,
                                    Double temperature,
                                    Double topP,
                                    Integer maxTokens,
                                    ChatStreamHandler handler,
                                    ToolStreamObserver toolObserver) {
        if (tools == null || tools.isEmpty()) {
            streamChat(config, turns, temperature, topP, maxTokens, handler);
            return;
        }
        List<ChatTurn> workingTurns = new ArrayList<>(turns == null ? List.of() : turns);
        prependToolInstruction(workingTurns, tools);
        boolean toolsUsed = false;
        for (int round = 0; round < 5; round++) {
            String response = chat(config, workingTurns, temperature, topP, maxTokens);
            ToolCall toolCall = parseToolCall(response);
            if (toolCall == null) {
                if (!toolsUsed) {
                    emitKnownAnswerAsStream(response, handler);
                } else {
                    streamChat(config, workingTurns, temperature, topP, maxTokens, handler);
                }
                return;
            }
            toolsUsed = true;
            String toolResult = toolExecutor.apply(toolCall);
            if (toolObserver != null) {
                toolObserver.onToolRound(toolCall, toolResult);
            }
            workingTurns.add(new ChatTurn("ASSISTANT", response));
            workingTurns.add(new ChatTurn("USER", "工具 " + toolCall.toolKey() + " 的执行结果：\n" + toolResult
                    + "\n请基于结果继续回答用户。"));
        }
        handler.onError(new BusinessException(ErrorCode.EXECUTION_FAILED, "工具调用超过最大轮次"));
    }

    private void emitKnownAnswerAsStream(String answer, ChatStreamHandler handler) {
        if (answer == null || answer.isEmpty()) {
            handler.onComplete();
            return;
        }
        int chunkSize = 16;
        for (int index = 0; index < answer.length(); index += chunkSize) {
            handler.onPartial(answer.substring(index, Math.min(index + chunkSize, answer.length())));
        }
        handler.onComplete();
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
