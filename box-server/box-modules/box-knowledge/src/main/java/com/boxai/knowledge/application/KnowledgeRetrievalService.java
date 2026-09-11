package com.boxai.knowledge.application;

import com.boxai.domain.agent.AgentKnowledge;
import com.boxai.domain.agent.AgentKnowledgeRepository;
import com.boxai.domain.knowledge.KnowledgeChunk;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class KnowledgeRetrievalService {

    private final AgentKnowledgeRepository agentKnowledgeRepository;
    private final KnowledgeSearchService knowledgeSearchService;

    public KnowledgeRetrievalService(AgentKnowledgeRepository agentKnowledgeRepository,
                                     KnowledgeSearchService knowledgeSearchService) {
        this.agentKnowledgeRepository = agentKnowledgeRepository;
        this.knowledgeSearchService = knowledgeSearchService;
    }

    public String buildContext(Long versionId, String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return null;
        }
        List<AgentKnowledge> bindings = agentKnowledgeRepository.listByVersionId(versionId);
        if (bindings.isEmpty()) {
            return null;
        }
        Set<Long> seenChunkIds = new LinkedHashSet<>();
        List<String> snippets = new ArrayList<>();
        for (AgentKnowledge binding : bindings) {
            int topK = binding.getTopK() == null ? 5 : binding.getTopK();
            List<KnowledgeChunk> chunks = knowledgeSearchService.searchChunks(
                    binding.getKnowledgeBaseId(), userMessage, topK);
            for (KnowledgeChunk chunk : chunks) {
                if (chunk.getId() != null && !seenChunkIds.add(chunk.getId())) {
                    continue;
                }
                snippets.add(chunk.getContent());
            }
        }
        if (snippets.isEmpty()) {
            return null;
        }
        return String.join("\n\n---\n\n", snippets);
    }
}
