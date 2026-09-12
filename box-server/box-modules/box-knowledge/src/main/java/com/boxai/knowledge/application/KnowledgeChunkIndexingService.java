package com.boxai.knowledge.application;

import com.boxai.ai.EmbeddingModelGateway;
import com.boxai.ai.ModelRuntimeConfig;
import com.boxai.domain.knowledge.KnowledgeBase;
import com.boxai.domain.knowledge.KnowledgeChunk;
import com.boxai.domain.knowledge.KnowledgeChunkSearchIndex;
import com.boxai.model.application.PlatformModelApplicationService;
import com.boxai.model.platform.ResolvedPlatformModel;
import com.boxai.tenant.application.QuotaApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class KnowledgeChunkIndexingService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeChunkIndexingService.class);

    private final KnowledgeChunkSearchIndex searchIndex;
    private final EmbeddingModelGateway embeddingModelGateway;
    private final PlatformModelApplicationService platformModelApplicationService;
    private final QuotaApplicationService quotaApplicationService;
    private final String defaultEmbeddingModel;

    public KnowledgeChunkIndexingService(KnowledgeChunkSearchIndex searchIndex,
                                         EmbeddingModelGateway embeddingModelGateway,
                                         PlatformModelApplicationService platformModelApplicationService,
                                         QuotaApplicationService quotaApplicationService,
                                         @Value("${box.ai.embedding.model-name:text-embedding-3-small}") String defaultEmbeddingModel) {
        this.searchIndex = searchIndex;
        this.embeddingModelGateway = embeddingModelGateway;
        this.platformModelApplicationService = platformModelApplicationService;
        this.quotaApplicationService = quotaApplicationService;
        this.defaultEmbeddingModel = defaultEmbeddingModel;
    }

    public void indexChunks(KnowledgeBase knowledgeBase, List<KnowledgeChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        searchIndex.ensureIndex();
        ModelRuntimeConfig embeddingConfig = resolveEmbeddingConfig(knowledgeBase);
        if (embeddingConfig == null) {
            log.info("Skip vector indexing for knowledge base {}: embedding config unavailable", knowledgeBase.getId());
            return;
        }
        List<String> texts = chunks.stream().map(KnowledgeChunk::getContent).toList();
        long estimatedTokens = texts.stream().mapToLong(text -> text == null ? 0 : text.length()).sum();
        quotaApplicationService.assertAiQuotaAvailable(knowledgeBase.getWorkspaceId());
        List<float[]> vectors;
        try {
            vectors = embeddingModelGateway.embedAll(embeddingConfig, texts);
            quotaApplicationService.consumeEmbeddingUsage(
                    knowledgeBase.getWorkspaceId(), Math.max(estimatedTokens, 1L));
        } catch (Exception e) {
            log.warn("Embedding failed for knowledge base {}: {}", knowledgeBase.getId(), e.getMessage());
            return;
        }
        for (int i = 0; i < chunks.size(); i++) {
            float[] vector = i < vectors.size() ? vectors.get(i) : null;
            if (vector != null && vector.length > 0) {
                searchIndex.indexChunk(chunks.get(i), vector);
            }
        }
    }

    public void deleteDocumentIndex(Long documentId) {
        searchIndex.deleteByDocument(documentId);
    }

    public void deleteKnowledgeBaseIndex(Long knowledgeBaseId) {
        searchIndex.deleteByKnowledgeBase(knowledgeBaseId);
    }

    public float[] embedQuery(KnowledgeBase knowledgeBase, String query) {
        ModelRuntimeConfig embeddingConfig = resolveEmbeddingConfig(knowledgeBase);
        if (embeddingConfig == null || query == null || query.isBlank()) {
            return new float[0];
        }
        try {
            quotaApplicationService.assertAiQuotaAvailable(knowledgeBase.getWorkspaceId());
            float[] vector = embeddingModelGateway.embed(embeddingConfig, query);
            quotaApplicationService.consumeEmbeddingUsage(
                    knowledgeBase.getWorkspaceId(), Math.max(query.length(), 1L));
            return vector;
        } catch (Exception e) {
            log.warn("Query embedding failed: {}", e.getMessage());
            return new float[0];
        }
    }

    private ModelRuntimeConfig resolveEmbeddingConfig(KnowledgeBase knowledgeBase) {
        Long modelId = knowledgeBase.getEmbeddingModelId();
        if (modelId == null) {
            modelId = platformModelApplicationService.findFirstRunnableModelId().orElse(null);
        }
        if (modelId == null) {
            return null;
        }
        ResolvedPlatformModel resolved = platformModelApplicationService.resolveForChat(modelId);
        ModelRuntimeConfig chatConfig = resolved.runtimeConfig();
        return new ModelRuntimeConfig(chatConfig.baseUrl(), chatConfig.apiKey(), defaultEmbeddingModel);
    }
}
