package com.boxai.ai;

/**
 * Extracts plain text from image bytes via OCR or vision-language models (OpenAI-compatible chat vision).
 */
public interface OcrModelGateway {

    String recognize(ModelRuntimeConfig config, byte[] imageBytes, String mimeType, String prompt);
}
