package com.boxai.domain.knowledge;

import java.util.List;

public interface KnowledgeChunkRepository {

    void saveBatch(List<KnowledgeChunk> chunks);

    List<KnowledgeChunk> searchByKeyword(Long knowledgeBaseId, String keyword, int limit);

    List<KnowledgeChunk> findByIds(List<Long> ids);

    void deleteByDocument(Long documentId);

    List<KnowledgeChunk> listByDocument(Long documentId);

    int countByKnowledgeBase(Long knowledgeBaseId);
}
