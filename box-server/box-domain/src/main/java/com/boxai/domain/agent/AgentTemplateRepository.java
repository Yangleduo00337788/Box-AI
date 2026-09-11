package com.boxai.domain.agent;

import java.util.List;
import java.util.Optional;

public interface AgentTemplateRepository {

    AgentTemplate save(AgentTemplate template);

    void update(AgentTemplate template);

    Optional<AgentTemplate> findById(Long id);

    List<AgentTemplate> listAll();

    List<AgentTemplate> listListed();

    void incrementInstallCount(Long id);
}
