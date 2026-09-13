package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.publish.EmbedCustomDomain;
import com.boxai.domain.publish.EmbedCustomDomainRepository;
import com.boxai.infrastructure.persistence.entity.EmbedCustomDomainDO;
import com.boxai.infrastructure.persistence.mapper.EmbedCustomDomainMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class EmbedCustomDomainRepositoryImpl implements EmbedCustomDomainRepository {

    private final EmbedCustomDomainMapper mapper;

    public EmbedCustomDomainRepositoryImpl(EmbedCustomDomainMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<EmbedCustomDomain> findByAgentId(Long agentId) {
        EmbedCustomDomainDO row = mapper.selectOneByQuery(QueryWrapper.create().eq("agent_id", agentId));
        return Optional.ofNullable(row).map(this::toDomain);
    }

    @Override
    public Optional<EmbedCustomDomain> findByDomain(String domain) {
        EmbedCustomDomainDO row = mapper.selectOneByQuery(QueryWrapper.create().eq("domain", domain));
        return Optional.ofNullable(row).map(this::toDomain);
    }

    @Override
    public EmbedCustomDomain save(EmbedCustomDomain domain) {
        EmbedCustomDomainDO row = new EmbedCustomDomainDO();
        row.setWorkspaceId(domain.getWorkspaceId());
        row.setAgentId(domain.getAgentId());
        row.setDomain(domain.getDomain());
        row.setVerifyToken(domain.getVerifyToken());
        row.setVerified(domain.getVerified() == null ? 0 : domain.getVerified());
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        row.setDeleted(0);
        mapper.insert(row);
        domain.setId(row.getId());
        return domain;
    }

    @Override
    public void update(EmbedCustomDomain domain) {
        EmbedCustomDomainDO patch = new EmbedCustomDomainDO();
        patch.setId(domain.getId());
        patch.setDomain(domain.getDomain());
        patch.setVerifyToken(domain.getVerifyToken());
        patch.setVerified(domain.getVerified());
        patch.setUpdatedAt(LocalDateTime.now());
        mapper.update(patch);
    }

    @Override
    public void deleteByAgentId(Long agentId) {
        mapper.deleteByQuery(QueryWrapper.create().eq("agent_id", agentId));
    }

    private EmbedCustomDomain toDomain(EmbedCustomDomainDO row) {
        EmbedCustomDomain domain = new EmbedCustomDomain();
        domain.setId(row.getId());
        domain.setWorkspaceId(row.getWorkspaceId());
        domain.setAgentId(row.getAgentId());
        domain.setDomain(row.getDomain());
        domain.setVerifyToken(row.getVerifyToken());
        domain.setVerified(row.getVerified());
        return domain;
    }
}
