package com.boxai.domain.knowledge;

import java.util.List;
import java.util.Optional;

public interface KnowledgeBaseRepository {

    KnowledgeBase save(KnowledgeBase knowledgeBase);

    void update(KnowledgeBase knowledgeBase);

    Optional<KnowledgeBase> findById(Long id);

    List<KnowledgeBase> listByWorkspace(Long workspaceId);

    void delete(Long id);
}
