package com.boxai.common.ai;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Heuristic model capability labels (aligned with box-web modelCapability.ts).
 */
public final class PlatformModelClassifier {

    private static final Pattern EMBEDDING = Pattern.compile("embed|embedding|\\bbge[-._/]|\\be5[-._]|gte[-._]", Pattern.CASE_INSENSITIVE);
    private static final Pattern RERANK = Pattern.compile("rerank|reranker", Pattern.CASE_INSENSITIVE);
    private static final Pattern OCR = Pattern.compile("\\bocr\\b|paddleocr", Pattern.CASE_INSENSITIVE);
    private static final Pattern VISION = Pattern.compile(
            "(\\bvl\\b|vision|\\bomni\\b|gpt-4o|gpt-4\\.1|gpt-5|internvl|qwen3-vl|qwen2\\.5-vl|qwen2-vl|glm-4v|glm-4\\.5v|4\\.5v\\b|\\b4v\\b|kimi-vl|step-1v|step-1o|\\bqvq\\b|gemini|claude-3|claude-4|gpt-4\\.5|多模态)",
            Pattern.CASE_INSENSITIVE);

    private PlatformModelClassifier() {
    }

    public static String combinedText(String modelCode, String modelName, String description) {
        return ((modelCode == null ? "" : modelCode) + " "
                + (modelName == null ? "" : modelName) + " "
                + (description == null ? "" : description)).toLowerCase(Locale.ROOT);
    }

    public static boolean isEmbeddingModel(String modelCode, String modelName, String description) {
        return EMBEDDING.matcher(combinedText(modelCode, modelName, description)).find();
    }

    public static boolean isRerankModel(String modelCode, String modelName, String description) {
        return RERANK.matcher(combinedText(modelCode, modelName, description)).find();
    }

    public static boolean isOcrModel(String modelCode, String modelName, String description) {
        return OCR.matcher(combinedText(modelCode, modelName, description)).find();
    }

    public static boolean isVisionModel(String modelCode, String modelName, String description) {
        return VISION.matcher(combinedText(modelCode, modelName, description)).find();
    }

    public static boolean isOcrOrVisionModel(String modelCode, String modelName, String description) {
        String text = combinedText(modelCode, modelName, description);
        return OCR.matcher(text).find() || VISION.matcher(text).find();
    }
}
