package com.boxai.domain.agent;

import java.util.List;
import java.util.Optional;

public interface AgentKnowledgeRepository {

    AgentKnowledge save(AgentKnowledge binding);

    void delete(Long id);

    Optional<AgentKnowledge> findByVersionAndKnowledgeBase(Long versionId, Long knowledgeBaseId);

    List<AgentKnowledge> listByVersionId(Long versionId);

    int countByKnowledgeBaseId(Long knowledgeBaseId);
}
