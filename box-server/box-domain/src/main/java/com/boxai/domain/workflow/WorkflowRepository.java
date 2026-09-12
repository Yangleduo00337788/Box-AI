package com.boxai.domain.workflow;

import java.util.List;
import java.util.Optional;

public interface WorkflowRepository {

    Workflow save(Workflow workflow);

    void update(Workflow workflow);

    Optional<Workflow> findById(Long id);

    Optional<Workflow> findByWebhookToken(String webhookToken);

    List<Workflow> listByWorkspace(Long workspaceId);

    List<Workflow> searchByName(Long workspaceId, String keyword, int limit);

    void delete(Long id);
}
