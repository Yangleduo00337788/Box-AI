package com.boxai.knowledge.application;

import com.boxai.ai.ModelRuntimeConfig;
import com.boxai.ai.RerankModelGateway;
import com.boxai.ai.RerankScore;
import com.boxai.common.constant.PermissionCodes;
import com.boxai.domain.knowledge.KnowledgeBase;
import com.boxai.domain.knowledge.KnowledgeBaseRepository;
import com.boxai.domain.knowledge.KnowledgeChunk;
import com.boxai.domain.knowledge.KnowledgeChunkRepository;
import com.boxai.domain.knowledge.KnowledgeChunkSearchIndex;
import com.boxai.knowledge.api.KnowledgeSearchHitVO;
import com.boxai.knowledge.api.KnowledgeSearchRequest;
import com.boxai.security.permission.WorkspacePermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class KnowledgeSearchService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeSearchService.class);

    private final KnowledgeBaseApplicationService knowledgeBaseApplicationService;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final KnowledgeChunkSearchIndex searchIndex;
    private final KnowledgeChunkIndexingService chunkIndexingService;
    private final RerankModelGateway rerankModelGateway;
    private final WorkspacePermissionService workspacePermissionService;

    public KnowledgeSearchService(KnowledgeBaseApplicationService knowledgeBaseApplicationService,
                                  KnowledgeBaseRepository knowledgeBaseRepository,
                                  KnowledgeChunkRepository knowledgeChunkRepository,
                                  KnowledgeChunkSearchIndex searchIndex,
                                  KnowledgeChunkIndexingService chunkIndexingService,
                                  RerankModelGateway rerankModelGateway,
                                  WorkspacePermissionService workspacePermissionService) {
        this.knowledgeBaseApplicationService = knowledgeBaseApplicationService;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.knowledgeChunkRepository = knowledgeChunkRepository;
        this.searchIndex = searchIndex;
        this.chunkIndexingService = chunkIndexingService;
        this.rerankModelGateway = rerankModelGateway;
        this.workspacePermissionService = workspacePermissionService;
    }

    public List<KnowledgeSearchHitVO> search(Long knowledgeBaseId, KnowledgeSearchRequest request) {
        workspacePermissionService.requirePermission(PermissionCodes.KNOWLEDGE_READ);
        KnowledgeBase knowledgeBase = knowledgeBaseApplicationService.requireKnowledgeBase(knowledgeBaseId);
        boolean rerank = knowledgeBase.getRerankModelId() != null;
        return searchHits(knowledgeBaseId, request.query(), request.topK(), "HYBRID", null, rerank);
    }

    public List<KnowledgeSearchHitVO> searchHits(Long knowledgeBaseId,
                                                 String query,
                                                 int topK,
                                                 String retrievalMode,
                                                 Double scoreThreshold,
                                                 boolean rerank) {
        int limit = topK < 1 ? 5 : topK;
        int candidateLimit = rerank ? Math.min(Math.max(limit * 4, 20), 50) : limit;
        List<KnowledgeChunk> chunks = searchChunksInternal(knowledgeBaseId, query, candidateLimit, retrievalMode);
        if (chunks.isEmpty()) {
            return List.of();
        }

        List<KnowledgeSearchHitVO> hits = new ArrayList<>(chunks.size());
        List<String> documents = new ArrayList<>(chunks.size());
        for (KnowledgeChunk chunk : chunks) {
            hits.add(new KnowledgeSearchHitVO(
                    chunk.getId(),
                    chunk.getDocumentId(),
                    chunk.getChunkIndex(),
                    chunk.getContent(),
                    score(chunk.getContent(), query)));
            documents.add(chunk.getContent());
        }

        if (rerank) {
            KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(knowledgeBaseId).orElse(null);
            ModelRuntimeConfig rerankConfig = knowledgeBase == null ? null : chunkIndexingService.resolveRerankConfig(knowledgeBase);
            if (rerankConfig != null) {
                try {
                    List<RerankScore> reranked = rerankModelGateway.rerank(rerankConfig, query, documents, candidateLimit);
                    if (!reranked.isEmpty()) {
                        List<KnowledgeSearchHitVO> rerankedHits = new ArrayList<>();
                        for (RerankScore item : reranked) {
                            if (item.index() < 0 || item.index() >= hits.size()) {
                                continue;
                            }
                            KnowledgeSearchHitVO original = hits.get(item.index());
                            rerankedHits.add(new KnowledgeSearchHitVO(
                                    original.chunkId(),
                                    original.documentId(),
                                    original.chunkIndex(),
                                    original.content(),
                                    item.score()));
                        }
                        if (!rerankedHits.isEmpty()) {
                            hits = rerankedHits;
                        }
                    }
                } catch (Exception e) {
                    log.warn("Rerank failed for knowledge base {}, fallback to hybrid score: {}",
                            knowledgeBaseId, e.getMessage());
                }
            }
        }

        return hits.stream()
                .filter(hit -> scoreThreshold == null || hit.score() >= scoreThreshold)
                .sorted((left, right) -> Double.compare(right.score(), left.score()))
                .limit(limit)
                .toList();
    }

    public List<KnowledgeChunk> searchChunks(Long knowledgeBaseId, String query, int topK) {
        return searchChunksInternal(knowledgeBaseId, query, topK, "HYBRID");
    }

    private List<KnowledgeChunk> searchChunksInternal(Long knowledgeBaseId, String query, int topK, String retrievalMode) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        knowledgeBaseApplicationService.requireKnowledgeBase(knowledgeBaseId);
        int limit = topK < 1 ? 5 : topK;
        String trimmedQuery = query.trim();
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(knowledgeBaseId).orElse(null);
        if (knowledgeBase == null) {
            return List.of();
        }

        String mode = retrievalMode == null ? "HYBRID" : retrievalMode.trim().toUpperCase();
        Set<Long> chunkIds = new LinkedHashSet<>();
        float[] queryVector = chunkIndexingService.embedQuery(knowledgeBase, trimmedQuery);
        if (!"KEYWORD".equals(mode) && queryVector.length > 0) {
            chunkIds.addAll(searchIndex.searchByVector(knowledgeBaseId, queryVector, limit));
        }
        if (!"VECTOR".equals(mode)) {
            chunkIds.addAll(searchIndex.searchByKeyword(knowledgeBaseId, trimmedQuery, limit));
        }

        if (!chunkIds.isEmpty()) {
            List<KnowledgeChunk> chunks = knowledgeChunkRepository.findByIds(new ArrayList<>(chunkIds));
            if (chunks.size() >= limit) {
                return chunks.subList(0, Math.min(limit, chunks.size()));
            }
            if (!chunks.isEmpty()) {
                return chunks;
            }
        }

        return knowledgeChunkRepository.searchByKeyword(knowledgeBaseId, trimmedQuery, limit);
    }

    private double score(String content, String query) {
        if (content == null || query == null || query.isBlank()) {
            return 0D;
        }
        String lowerContent = content.toLowerCase();
        String lowerQuery = query.toLowerCase();
        int occurrences = 0;
        int index = 0;
        while ((index = lowerContent.indexOf(lowerQuery, index)) >= 0) {
            occurrences++;
            index += lowerQuery.length();
        }
        return occurrences > 0 ? occurrences : (lowerContent.contains(lowerQuery) ? 1D : 0.5D);
    }
}
