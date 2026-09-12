package com.boxai.domain.knowledge;

import java.util.List;
import java.util.Optional;

public interface KnowledgeBaseRepository {

    KnowledgeBase save(KnowledgeBase knowledgeBase);

    void update(KnowledgeBase knowledgeBase);

    Optional<KnowledgeBase> findById(Long id);

    List<KnowledgeBase> listByWorkspace(Long workspaceId);

    int countByWorkspace(Long workspaceId);

    int countByTenantId(Long tenantId);

    List<KnowledgeBase> searchByName(Long workspaceId, String keyword, int limit);

    void delete(Long id);
}
