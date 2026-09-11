package com.boxai.domain.knowledge;

import java.util.List;

public interface KnowledgeChunkSearchIndex {

    void ensureIndex();

    void indexChunk(KnowledgeChunk chunk, float[] embedding);

    void deleteByDocument(Long documentId);

    void deleteByKnowledgeBase(Long knowledgeBaseId);

    List<Long> searchByVector(Long knowledgeBaseId, float[] queryEmbedding, int topK);

    List<Long> searchByKeyword(Long knowledgeBaseId, String keyword, int topK);
}
