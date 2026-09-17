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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class KnowledgeSearchService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeSearchService.class);
    private static final int RRF_K = 60;

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
        List<ScoredChunk> scoredChunks = searchChunksInternal(knowledgeBaseId, query, candidateLimit, retrievalMode);
        if (scoredChunks.isEmpty()) {
            return List.of();
        }

        List<KnowledgeSearchHitVO> hits = new ArrayList<>(scoredChunks.size());
        List<String> documents = new ArrayList<>(scoredChunks.size());
        for (ScoredChunk scored : scoredChunks) {
            KnowledgeChunk chunk = scored.chunk();
            hits.add(new KnowledgeSearchHitVO(
                    chunk.getId(),
                    chunk.getDocumentId(),
                    chunk.getChunkIndex(),
                    chunk.getContent(),
                    scored.score()));
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
        return searchChunksInternal(knowledgeBaseId, query, topK, "HYBRID").stream()
                .map(ScoredChunk::chunk)
                .toList();
    }

    private List<ScoredChunk> searchChunksInternal(Long knowledgeBaseId, String query, int topK, String retrievalMode) {
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
        Map<Long, Double> fusedScores = new LinkedHashMap<>();
        float[] queryVector = chunkIndexingService.embedQuery(knowledgeBase, trimmedQuery);
        if (!"KEYWORD".equals(mode) && queryVector.length > 0) {
            List<Long> vectorIds = searchIndex.searchByVector(knowledgeBaseId, queryVector, limit);
            accumulateRrf(fusedScores, vectorIds);
        }
        if (!"VECTOR".equals(mode)) {
            List<Long> keywordIds = searchIndex.searchByKeyword(knowledgeBaseId, trimmedQuery, limit);
            accumulateRrf(fusedScores, keywordIds);
        }

        if (!fusedScores.isEmpty()) {
            List<Long> rankedIds = fusedScores.entrySet().stream()
                    .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                    .limit(limit)
                    .map(Map.Entry::getKey)
                    .toList();
            Map<Long, KnowledgeChunk> chunkMap = new HashMap<>();
            for (KnowledgeChunk chunk : knowledgeChunkRepository.findByIds(new ArrayList<>(rankedIds))) {
                chunkMap.put(chunk.getId(), chunk);
            }
            List<ScoredChunk> ranked = new ArrayList<>();
            for (Long chunkId : rankedIds) {
                KnowledgeChunk chunk = chunkMap.get(chunkId);
                if (chunk != null) {
                    ranked.add(new ScoredChunk(chunk, fusedScores.getOrDefault(chunkId, 0D)));
                }
            }
            if (!ranked.isEmpty()) {
                return ranked;
            }
        }

        return knowledgeChunkRepository.searchByKeyword(knowledgeBaseId, trimmedQuery, limit).stream()
                .map(chunk -> new ScoredChunk(chunk, keywordScore(chunk.getContent(), trimmedQuery)))
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .toList();
    }

    private void accumulateRrf(Map<Long, Double> fusedScores, List<Long> rankedIds) {
        for (int index = 0; index < rankedIds.size(); index++) {
            Long chunkId = rankedIds.get(index);
            if (chunkId == null) {
                continue;
            }
            fusedScores.merge(chunkId, 1.0D / (RRF_K + index + 1), Double::sum);
        }
    }

    private double keywordScore(String content, String query) {
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
        if (occurrences > 0) {
            return occurrences;
        }
        return lowerContent.contains(lowerQuery) ? 1D : 0D;
    }

    private record ScoredChunk(KnowledgeChunk chunk, double score) {
    }
}
