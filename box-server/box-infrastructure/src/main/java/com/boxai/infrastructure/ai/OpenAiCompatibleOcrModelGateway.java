package com.boxai.infrastructure.ai;

import com.boxai.ai.ModelRuntimeConfig;
import com.boxai.ai.OcrModelGateway;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.common.security.SsrfGuard;
import com.boxai.common.security.SsrfSafeHttpClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Component
public class OpenAiCompatibleOcrModelGateway implements OcrModelGateway {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = SsrfSafeHttpClient.create(Duration.ofSeconds(10), false);

    @Override
    public String recognize(ModelRuntimeConfig config, byte[] imageBytes, String mimeType, String prompt) {
        if (config == null || config.apiKey() == null || config.apiKey().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "OCR API Key 未配置");
        }
        if (imageBytes == null || imageBytes.length == 0) {
            return "";
        }
        String modelName = config.modelName() == null || config.modelName().isBlank()
                ? "gpt-4o-mini"
                : config.modelName();
        String safeMime = mimeType == null || mimeType.isBlank() ? "image/png" : mimeType;
        String dataUrl = "data:" + safeMime + ";base64," + Base64.getEncoder().encodeToString(imageBytes);

        ObjectNode textPart = OBJECT_MAPPER.createObjectNode();
        textPart.put("type", "text");
        textPart.put("text", prompt);

        ObjectNode imageUrl = OBJECT_MAPPER.createObjectNode();
        imageUrl.put("url", dataUrl);
        ObjectNode imagePart = OBJECT_MAPPER.createObjectNode();
        imagePart.put("type", "image_url");
        imagePart.set("image_url", imageUrl);

        ArrayNode content = OBJECT_MAPPER.createArrayNode().add(textPart).add(imagePart);
        ObjectNode userMessage = OBJECT_MAPPER.createObjectNode();
        userMessage.put("role", "user");
        userMessage.set("content", content);

        ObjectNode body = OBJECT_MAPPER.createObjectNode();
        body.put("model", modelName);
        body.put("temperature", 0);
        body.put("max_tokens", 4096);
        body.set("messages", OBJECT_MAPPER.createArrayNode().add(userMessage));

        URI endpoint = SsrfGuard.validateHttpUrl(OpenAiCompatibleApiUrls.chatCompletionsUrl(config.baseUrl()));
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(120))
                .header("Authorization", "Bearer " + config.apiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = SsrfSafeHttpClient.send(HTTP_CLIENT, request, true, Duration.ofSeconds(120));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(ErrorCode.EXECUTION_FAILED, "OCR 请求失败: HTTP " + response.statusCode());
            }
            JsonNode root = OBJECT_MAPPER.readTree(response.body());
            String text = root.path("choices").path(0).path("message").path("content").asText("");
            return text == null ? "" : text.trim();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.EXECUTION_FAILED, "OCR 调用失败: " + ex.getMessage());
        }
    }

}
