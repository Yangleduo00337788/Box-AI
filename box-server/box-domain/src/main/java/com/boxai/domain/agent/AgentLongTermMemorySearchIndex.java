package com.boxai.domain.agent;

import java.util.List;

public interface AgentLongTermMemorySearchIndex {

    void ensureIndex();

    void indexMemory(AgentLongTermMemory memory, float[] embedding);

    void deleteMemory(String esDocumentId);

    List<Long> searchByVector(Long agentId, Long userId, float[] queryEmbedding, int topK);
}
