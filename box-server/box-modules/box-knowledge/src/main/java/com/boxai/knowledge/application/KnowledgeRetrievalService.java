package com.boxai.knowledge.application;

import com.boxai.domain.agent.AgentKnowledge;
import com.boxai.domain.agent.AgentKnowledgeRepository;
import com.boxai.domain.knowledge.KnowledgeDocument;
import com.boxai.domain.knowledge.KnowledgeDocumentRepository;
import com.boxai.knowledge.api.KnowledgeCitationVO;
import com.boxai.knowledge.api.KnowledgeSearchHitVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class KnowledgeRetrievalService {

    private final AgentKnowledgeRepository agentKnowledgeRepository;
    private final KnowledgeSearchService knowledgeSearchService;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;

    public KnowledgeRetrievalService(AgentKnowledgeRepository agentKnowledgeRepository,
                                     KnowledgeSearchService knowledgeSearchService,
                                     KnowledgeDocumentRepository knowledgeDocumentRepository) {
        this.agentKnowledgeRepository = agentKnowledgeRepository;
        this.knowledgeSearchService = knowledgeSearchService;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
    }

    public String buildContext(Long versionId, String userMessage) {
        return retrieve(versionId, userMessage).context();
    }

    public KnowledgeRetrievalResult retrieve(Long versionId, String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return KnowledgeRetrievalResult.empty();
        }
        List<AgentKnowledge> bindings = agentKnowledgeRepository.listByVersionId(versionId);
        if (bindings.isEmpty()) {
            return KnowledgeRetrievalResult.empty();
        }

        Map<Long, KnowledgeSearchHitVO> mergedHits = new LinkedHashMap<>();
        for (AgentKnowledge binding : bindings) {
            int topK = binding.getTopK() == null ? 5 : binding.getTopK();
            Double threshold = toDouble(binding.getScoreThreshold());
            boolean rerank = !Boolean.FALSE.equals(binding.getRerankEnabled());
            List<KnowledgeSearchHitVO> hits = knowledgeSearchService.searchHits(
                    binding.getKnowledgeBaseId(),
                    userMessage,
                    topK,
                    binding.getRetrievalMode(),
                    threshold,
                    rerank);
            for (KnowledgeSearchHitVO hit : hits) {
                KnowledgeSearchHitVO existing = mergedHits.get(hit.chunkId());
                if (existing == null || hit.score() > existing.score()) {
                    mergedHits.put(hit.chunkId(), hit);
                }
            }
        }
        if (mergedHits.isEmpty()) {
            return KnowledgeRetrievalResult.empty();
        }

        List<KnowledgeSearchHitVO> rankedHits = mergedHits.values().stream()
                .sorted((left, right) -> Double.compare(right.score(), left.score()))
                .toList();

        Map<Long, String> documentNames = resolveDocumentNames(rankedHits);
        List<String> snippets = new ArrayList<>();
        List<KnowledgeCitationVO> citations = new ArrayList<>();
        int index = 1;
        for (KnowledgeSearchHitVO hit : rankedHits) {
            String documentName = documentNames.getOrDefault(hit.documentId(), "document-" + hit.documentId());
            String label = "[" + index + "] " + documentName
                    + (hit.chunkIndex() != null ? " #chunk-" + hit.chunkIndex() : "");
            snippets.add(label + "\n" + hit.content());
            citations.add(new KnowledgeCitationVO(
                    index,
                    hit.chunkId(),
                    hit.documentId(),
                    documentName,
                    null,
                    hit.chunkIndex(),
                    hit.content(),
                    hit.score()));
            index++;
        }

        boolean citationEnabled = bindings.stream().anyMatch(item -> !Boolean.FALSE.equals(item.getCitationEnabled()));
        String context = citationEnabled
                ? String.join("\n\n---\n\n", snippets)
                : String.join("\n\n---\n\n", rankedHits.stream().map(KnowledgeSearchHitVO::content).toList());
        return new KnowledgeRetrievalResult(context, citations);
    }

    private Map<Long, String> resolveDocumentNames(List<KnowledgeSearchHitVO> hits) {
        Map<Long, String> names = new LinkedHashMap<>();
        for (KnowledgeSearchHitVO hit : hits) {
            if (hit.documentId() == null || names.containsKey(hit.documentId())) {
                continue;
            }
            KnowledgeDocument document = knowledgeDocumentRepository.findById(hit.documentId()).orElse(null);
            if (document == null) {
                continue;
            }
            String name = document.getFileName();
            if (name == null || name.isBlank()) {
                name = document.getName();
            }
            names.put(hit.documentId(), name == null || name.isBlank() ? "document-" + hit.documentId() : name);
        }
        return names;
    }

    private Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }
}
