package com.boxai.agent.chat;

import com.boxai.ai.ModelRuntimeConfig;
import com.boxai.ai.OcrModelGateway;
import com.boxai.infrastructure.storage.PublicAssetImageLoader;
import com.boxai.model.application.PlatformOcrSettingsApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chat stores images as {@code [图片: name]\n/api/v1/public-assets/...} text; LLM APIs only see text unless enriched.
 */
@Component
public class ChatUserMessageImageEnricher {

    private static final Logger log = LoggerFactory.getLogger(ChatUserMessageImageEnricher.class);
    private static final Pattern IMAGE_BLOCK = Pattern.compile(
            "\\[图片:\\s*([^\\]]+)\\]\\s*\\n+((?:https?://[^\\s]+)?/api/v1/public-assets/[^\\s]+)",
            Pattern.MULTILINE);
    private static final String OCR_PROMPT = """
            请识别图片中的全部可见文字，按阅读顺序输出纯文本。
            保留段落与列表换行，不要添加解释。
            """;

    private final PublicAssetImageLoader publicAssetImageLoader;
    private final PlatformOcrSettingsApplicationService platformOcrSettingsApplicationService;
    private final OcrModelGateway ocrModelGateway;

    public ChatUserMessageImageEnricher(PublicAssetImageLoader publicAssetImageLoader,
                                      PlatformOcrSettingsApplicationService platformOcrSettingsApplicationService,
                                      OcrModelGateway ocrModelGateway) {
        this.publicAssetImageLoader = publicAssetImageLoader;
        this.platformOcrSettingsApplicationService = platformOcrSettingsApplicationService;
        this.ocrModelGateway = ocrModelGateway;
    }

    public String enrichWithImageText(String content) {
        if (content == null || content.isBlank() || !content.contains("[图片:")) {
            return content;
        }
        Matcher matcher = IMAGE_BLOCK.matcher(content);
        if (!matcher.find()) {
            return content;
        }
        matcher.reset();
        StringBuffer buffer = new StringBuffer();
        ModelRuntimeConfig ocrConfig = platformOcrSettingsApplicationService.resolveRuntimeConfig().orElse(null);
        while (matcher.find()) {
            String name = matcher.group(1).trim();
            String url = matcher.group(2).trim();
            String replacement = recognizeImageBlock(name, url, ocrConfig);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString().replaceAll("\\n{3,}", "\n\n").trim();
    }

    private String recognizeImageBlock(String name, String url, ModelRuntimeConfig ocrConfig) {
        if (ocrConfig == null) {
            return "[图片: " + name + "]\n（未配置平台 OCR/视觉模型，无法识别图片内容。请在管理端配置「平台 OCR 默认模型」。）";
        }
        var loaded = publicAssetImageLoader.loadFromReference(url);
        if (loaded.isEmpty()) {
            return "[图片: " + name + "]\n（图片加载失败，无法识别）";
        }
        try {
            PublicAssetImageLoader.LoadedPublicImage image = loaded.get();
            String text = ocrModelGateway.recognize(ocrConfig, image.bytes(), image.contentType(), OCR_PROMPT);
            if (text == null || text.isBlank()) {
                return "[图片: " + name + "]\n（未识别到文字）";
            }
            return "[图片: " + name + "]\n" + text.trim();
        } catch (Exception e) {
            log.warn("Chat image OCR failed for {}: {}", name, e.getMessage());
            return "[图片: " + name + "]\n（图片识别失败：" + e.getMessage() + "）";
        }
    }
}
