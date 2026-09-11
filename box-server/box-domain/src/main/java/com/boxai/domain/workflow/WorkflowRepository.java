package com.boxai.domain.workflow;

import java.util.List;
import java.util.Optional;

public interface WorkflowRepository {

    Workflow save(Workflow workflow);

    void update(Workflow workflow);

    Optional<Workflow> findById(Long id);

    List<Workflow> listByWorkspace(Long workspaceId);

    void delete(Long id);
}
