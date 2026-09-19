package com.boxai.infrastructure.ai;

import com.boxai.ai.ChatStreamHandler;
import com.boxai.ai.ChatTurn;
import com.boxai.ai.ModelRuntimeConfig;
import com.boxai.common.ai.PlatformModelClassifier;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.common.security.SsrfGuard;
import com.boxai.common.security.SsrfSafeHttpClient;
import com.boxai.infrastructure.storage.PublicAssetImageLoader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Vision chat via raw OpenAI-compatible HTTP (same image payload as {@link OpenAiCompatibleOcrModelGateway}).
 * Used for providers such as SiliconFlow where langchain4j multimodal serialization is unreliable.
 */
@Component
public class OpenAiCompatibleVisionChatHttpClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = SsrfSafeHttpClient.create(Duration.ofSeconds(10), false);

    private final PublicAssetImageLoader publicAssetImageLoader;

    public OpenAiCompatibleVisionChatHttpClient(PublicAssetImageLoader publicAssetImageLoader) {
        this.publicAssetImageLoader = publicAssetImageLoader;
    }

    public boolean shouldUseHttp(ModelRuntimeConfig config, List<ChatTurn> turns) {
        if (config == null || config.apiKey() == null || config.apiKey().isBlank()) {
            return false;
        }
        if (!PlatformModelClassifier.isOcrOrVisionModel(config.modelName(), null, null)) {
            return false;
        }
        if (turns == null || turns.isEmpty()) {
            return false;
        }
        return turns.stream().anyMatch(turn -> isUserTurnWithImages(turn));
    }

    public String chat(ModelRuntimeConfig config,
                       List<ChatTurn> turns,
                       Double temperature,
                       Double topP,
                       Integer maxTokens) {
        ObjectNode body = buildRequestBody(config, turns, temperature, topP, maxTokens, false);
        HttpResponse<String> response = post(config, body);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new BusinessException(ErrorCode.EXECUTION_FAILED,
                    "视觉模型请求失败: HTTP " + response.statusCode() + " " + truncate(response.body()));
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(response.body());
            String content = extractAssistantText(root);
            if (content == null || content.isBlank()) {
                throw new BusinessException(ErrorCode.EXECUTION_FAILED, "视觉模型返回空内容");
            }
            return content;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.EXECUTION_FAILED, "解析视觉模型响应失败: " + ex.getMessage());
        }
    }

    public void streamChat(ModelRuntimeConfig config,
                           List<ChatTurn> turns,
                           Double temperature,
                           Double topP,
                           Integer maxTokens,
                           ChatStreamHandler handler) {
        try {
            ObjectNode body = buildRequestBody(config, turns, temperature, topP, maxTokens, true);
            streamSseCompletions(config, body, handler);
        } catch (Exception ex) {
            handler.onError(ex);
        }
    }

    private void streamSseCompletions(ModelRuntimeConfig config, ObjectNode body, ChatStreamHandler handler) {
        URI endpoint = SsrfGuard.validateHttpUrl(OpenAiCompatibleApiUrls.chatCompletionsUrl(config.baseUrl()));
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(120))
                .header("Authorization", "Bearer " + config.apiKey())
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        HttpResponse<InputStream> response;
        try {
            response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (Exception ex) {
            handler.onError(new BusinessException(ErrorCode.EXECUTION_FAILED, "视觉模型流式请求失败: " + ex.getMessage()));
            return;
        }
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            String errBody = readStreamBody(response.body());
            handler.onError(new BusinessException(ErrorCode.EXECUTION_FAILED,
                    "视觉模型流式请求失败: HTTP " + status + " " + truncate(errBody)));
            return;
        }
        try (InputStream input = response.body();
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || !line.startsWith("data:")) {
                    continue;
                }
                String payload = line.substring(5).trim();
                if (payload.isEmpty()) {
                    continue;
                }
                if ("[DONE]".equals(payload)) {
                    break;
                }
                String delta = extractStreamDelta(payload);
                if (!delta.isEmpty()) {
                    handler.onPartial(delta);
                }
            }
            handler.onComplete();
        } catch (Exception ex) {
            handler.onError(new BusinessException(ErrorCode.EXECUTION_FAILED, "读取视觉模型流式响应失败: " + ex.getMessage()));
        }
    }

    private static String readStreamBody(InputStream input) {
        if (input == null) {
            return "";
        }
        try (input) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return "";
        }
    }

    private static String extractStreamDelta(String payload) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(payload);
            JsonNode delta = root.path("choices").path(0).path("delta");
            String content = delta.path("content").asText("");
            if (content != null && !content.isEmpty()) {
                return content;
            }
            String reasoning = delta.path("reasoning_content").asText("");
            return reasoning == null ? "" : reasoning;
        } catch (Exception ex) {
            return "";
        }
    }

    private HttpResponse<String> post(ModelRuntimeConfig config, ObjectNode body) {
        URI endpoint = SsrfGuard.validateHttpUrl(OpenAiCompatibleApiUrls.chatCompletionsUrl(config.baseUrl()));
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(120))
                .header("Authorization", "Bearer " + config.apiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        try {
            return SsrfSafeHttpClient.send(HTTP_CLIENT, request, true, Duration.ofSeconds(120));
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.EXECUTION_FAILED, "视觉模型请求失败: " + ex.getMessage());
        }
    }

    private ObjectNode buildRequestBody(ModelRuntimeConfig config,
                                        List<ChatTurn> turns,
                                        Double temperature,
                                        Double topP,
                                        Integer maxTokens,
                                        boolean stream) {
        ArrayNode messages = OBJECT_MAPPER.createArrayNode();
        for (ChatTurn turn : turns) {
            if (turn == null || turn.content() == null || turn.content().isBlank()) {
                continue;
            }
            String role = turn.role() == null ? "" : turn.role().toUpperCase();
            switch (role) {
                case "SYSTEM" -> {
                    ObjectNode message = OBJECT_MAPPER.createObjectNode();
                    message.put("role", "system");
                    message.put("content", turn.content());
                    messages.add(message);
                }
                case "ASSISTANT" -> {
                    ObjectNode message = OBJECT_MAPPER.createObjectNode();
                    message.put("role", "assistant");
                    message.put("content", turn.content());
                    messages.add(message);
                }
                case "USER" -> messages.add(buildUserMessageNode(turn.content()));
                default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的消息角色: " + turn.role());
            }
        }
        if (messages.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "消息内容不能为空");
        }
        ObjectNode body = OBJECT_MAPPER.createObjectNode();
        body.put("model", config.modelName());
        body.put("stream", stream);
        if (temperature != null) {
            body.put("temperature", temperature);
        }
        if (topP != null) {
            body.put("top_p", topP);
        }
        if (maxTokens != null) {
            body.put("max_tokens", maxTokens);
        }
        body.set("messages", messages);
        return body;
    }

    private ObjectNode buildUserMessageNode(String content) {
        ObjectNode message = OBJECT_MAPPER.createObjectNode();
        message.put("role", "user");
        List<ChatUserMessageMultimodalSupport.UserContentPart> parts =
                ChatUserMessageMultimodalSupport.buildContentParts(content, publicAssetImageLoader);
        if (parts.size() == 1 && "text".equals(parts.get(0).type())) {
            message.put("content", parts.get(0).text());
            return message;
        }
        ArrayNode contentParts = OBJECT_MAPPER.createArrayNode();
        for (ChatUserMessageMultimodalSupport.UserContentPart part : parts) {
            if ("text".equals(part.type())) {
                ObjectNode textPart = OBJECT_MAPPER.createObjectNode();
                textPart.put("type", "text");
                textPart.put("text", part.text());
                contentParts.add(textPart);
            } else if ("image".equals(part.type())) {
                ObjectNode imageUrl = OBJECT_MAPPER.createObjectNode();
                imageUrl.put("url", part.dataUrl());
                ObjectNode imagePart = OBJECT_MAPPER.createObjectNode();
                imagePart.put("type", "image_url");
                imagePart.set("image_url", imageUrl);
                contentParts.add(imagePart);
            }
        }
        message.set("content", contentParts);
        return message;
    }

    private static boolean isUserTurnWithImages(ChatTurn turn) {
        return turn != null
                && "USER".equalsIgnoreCase(turn.role())
                && ChatUserMessageMultimodalSupport.containsImageBlock(turn.content());
    }

    private static String extractAssistantText(JsonNode root) {
        JsonNode message = root.path("choices").path(0).path("message");
        String content = message.path("content").asText("");
        if (content != null && !content.isBlank()) {
            return content.trim();
        }
        String reasoning = message.path("reasoning_content").asText("");
        return reasoning == null ? "" : reasoning.trim();
    }

    private static String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 240 ? value : value.substring(0, 240) + "...";
    }
}
