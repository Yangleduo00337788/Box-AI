package com.boxai.infrastructure.ai;

import com.boxai.ai.EmbeddingModelGateway;
import com.boxai.ai.ModelRuntimeConfig;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class OpenAiCompatibleEmbeddingGateway implements EmbeddingModelGateway {

    @Override
    public float[] embed(ModelRuntimeConfig config, String text) {
        if (text == null || text.isBlank()) {
            return new float[0];
        }
        EmbeddingModel model = buildModel(config);
        Embedding embedding = model.embed(text).content();
        return embedding.vector();
    }

    @Override
    public List<float[]> embedAll(ModelRuntimeConfig config, List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        EmbeddingModel model = buildModel(config);
        List<TextSegment> segments = texts.stream().map(TextSegment::from).toList();
        List<Embedding> embeddings = model.embedAll(segments).content();
        List<float[]> vectors = new ArrayList<>(embeddings.size());
        for (Embedding embedding : embeddings) {
            vectors.add(embedding.vector());
        }
        return vectors;
    }

    private EmbeddingModel buildModel(ModelRuntimeConfig config) {
        if (config.apiKey() == null || config.apiKey().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Embedding API Key 未配置");
        }
        String modelName = config.modelName() == null || config.modelName().isBlank()
                ? "text-embedding-3-small"
                : config.modelName();
        return OpenAiEmbeddingModel.builder()
                .apiKey(config.apiKey())
                .baseUrl(normalizeBaseUrl(config.baseUrl()))
                .modelName(modelName)
                .timeout(Duration.ofSeconds(60))
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://api.openai.com/v1";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
