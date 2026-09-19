package com.boxai.knowledge.application;

import com.boxai.ai.ModelRuntimeConfig;
import com.boxai.ai.OcrModelGateway;
import com.boxai.domain.knowledge.KnowledgeBase;
import com.boxai.knowledge.support.ExtractedTextValidator;
import com.boxai.knowledge.support.PdfPageRasterizer;
import com.boxai.model.application.PlatformOcrSettingsApplicationService;
import com.boxai.tenant.application.QuotaApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Platform-managed OCR for knowledge ingestion (images / scanned PDF). Not configurable by workspace users.
 */
@Service
public class KnowledgeOcrService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeOcrService.class);
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "svg");
    private static final String DEFAULT_PROMPT = """
            请识别图片中的全部可见文字，按阅读顺序输出纯文本。
            保留段落与列表换行，不要添加解释或 Markdown 标题。
            """;

    private final OcrModelGateway ocrModelGateway;
    private final PlatformOcrSettingsApplicationService platformOcrSettingsApplicationService;
    private final QuotaApplicationService quotaApplicationService;
    private final boolean enabled;
    private final int minTextChars;
    private final int maxPdfPages;
    private final float pdfDpi;
    private final String prompt;

    public KnowledgeOcrService(OcrModelGateway ocrModelGateway,
                               PlatformOcrSettingsApplicationService platformOcrSettingsApplicationService,
                               QuotaApplicationService quotaApplicationService,
                               @Value("${box.ai.ocr.enabled:true}") boolean enabled,
                               @Value("${box.ai.ocr.min-text-chars:48}") int minTextChars,
                               @Value("${box.ai.ocr.max-pdf-pages:5}") int maxPdfPages,
                               @Value("${box.ai.ocr.pdf-dpi:144}") float pdfDpi,
                               @Value("${box.ai.ocr.prompt:}") String prompt) {
        this.ocrModelGateway = ocrModelGateway;
        this.platformOcrSettingsApplicationService = platformOcrSettingsApplicationService;
        this.quotaApplicationService = quotaApplicationService;
        this.enabled = enabled;
        this.minTextChars = minTextChars;
        this.maxPdfPages = maxPdfPages;
        this.pdfDpi = pdfDpi;
        this.prompt = prompt == null || prompt.isBlank() ? DEFAULT_PROMPT : prompt.trim();
    }

    public boolean mayNeedOcr(String fileName, String mimeType, String extractedText) {
        return needsOcr(fileName, extractedText);
    }

    public String enrichExtractedText(KnowledgeBase knowledgeBase,
                                      byte[] bytes,
                                      String fileName,
                                      String mimeType,
                                      String extractedText) {
        if (!enabled || knowledgeBase == null || bytes == null || bytes.length == 0) {
            return extractedText;
        }
        if (!needsOcr(fileName, extractedText)) {
            return extractedText;
        }
        if (IMAGE_EXTENSIONS.contains(extension(fileName))
                || ExtractedTextValidator.looksLikeBinaryNoise(extractedText)) {
            extractedText = "";
        }
        ModelRuntimeConfig config = resolveOcrConfig();
        if (config == null) {
            log.warn("OCR skipped for {}: no runnable OCR/vision model", fileName);
            return extractedText;
        }
        try {
            quotaApplicationService.assertAiQuotaAvailable(knowledgeBase.getWorkspaceId());
            String ocrText = recognizeDocument(config, bytes, fileName, mimeType);
            quotaApplicationService.consumeAiUsage(
                    knowledgeBase.getWorkspaceId(),
                    Math.max(ocrText == null ? 0 : ocrText.length(), 512L));
            return mergeText(extractedText, ocrText);
        } catch (Exception e) {
            log.warn("OCR failed for {}: {}", fileName, e.getMessage());
            return IMAGE_EXTENSIONS.contains(extension(fileName)) ? "" : extractedText;
        }
    }

    private String recognizeDocument(ModelRuntimeConfig config, byte[] bytes, String fileName, String mimeType) {
        String ext = extension(fileName);
        if ("pdf".equals(ext)) {
            return recognizePdf(config, bytes);
        }
        String imageMime = resolveImageMime(ext, mimeType);
        return ocrModelGateway.recognize(config, bytes, imageMime, prompt);
    }

    private String recognizePdf(ModelRuntimeConfig config, byte[] pdfBytes) {
        List<byte[]> pages = PdfPageRasterizer.renderPagesAsPng(pdfBytes, maxPdfPages, pdfDpi);
        if (pages.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < pages.size(); i++) {
            String pageText = ocrModelGateway.recognize(config, pages.get(i), "image/png", prompt);
            if (pageText != null && !pageText.isBlank()) {
                if (!builder.isEmpty()) {
                    builder.append("\n\n");
                }
                builder.append("--- page ").append(i + 1).append(" ---\n").append(pageText.trim());
            }
        }
        return builder.toString();
    }

    private boolean needsOcr(String fileName, String extractedText) {
        String ext = extension(fileName);
        if (IMAGE_EXTENSIONS.contains(ext)) {
            return true;
        }
        if ("pdf".equals(ext)) {
            int length = extractedText == null ? 0 : extractedText.trim().length();
            return length < minTextChars;
        }
        return false;
    }

    private ModelRuntimeConfig resolveOcrConfig() {
        return platformOcrSettingsApplicationService.resolveRuntimeConfig().orElse(null);
    }

    private String mergeText(String extractedText, String ocrText) {
        String base = extractedText == null ? "" : extractedText.trim();
        String ocr = ocrText == null ? "" : ocrText.trim();
        if (ExtractedTextValidator.looksLikeBinaryNoise(base)) {
            base = "";
        }
        if (ocr.isEmpty()) {
            return base;
        }
        if (base.isEmpty()) {
            return ocr;
        }
        if (base.contains(ocr) || ocr.contains(base)) {
            if (ExtractedTextValidator.looksLikeBinaryNoise(base)) {
                return ocr;
            }
            return base.length() >= ocr.length() ? base : ocr;
        }
        return base + "\n\n--- OCR ---\n" + ocr;
    }

    private String resolveImageMime(String ext, String mimeType) {
        if (mimeType != null && !mimeType.isBlank() && mimeType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return mimeType;
        }
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "svg" -> "image/svg+xml";
            default -> "image/png";
        };
    }

    private String extension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
