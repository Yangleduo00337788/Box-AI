package com.boxai.domain.knowledge;

import java.util.List;
import java.util.Optional;

public interface KnowledgeDocumentRepository {

    KnowledgeDocument save(KnowledgeDocument document);

    void update(KnowledgeDocument document);

    Optional<KnowledgeDocument> findById(Long id);

    List<KnowledgeDocument> listByKnowledgeBase(Long knowledgeBaseId);

    void delete(Long id);
}
