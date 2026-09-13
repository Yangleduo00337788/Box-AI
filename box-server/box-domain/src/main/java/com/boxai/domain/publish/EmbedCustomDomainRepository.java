package com.boxai.domain.publish;

import java.util.Optional;

public interface EmbedCustomDomainRepository {

    Optional<EmbedCustomDomain> findByAgentId(Long agentId);

    Optional<EmbedCustomDomain> findByDomain(String domain);

    EmbedCustomDomain save(EmbedCustomDomain domain);

    void update(EmbedCustomDomain domain);

    void deleteByAgentId(Long agentId);
}
