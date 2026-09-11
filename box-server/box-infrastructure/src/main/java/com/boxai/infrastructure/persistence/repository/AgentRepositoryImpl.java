package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.agent.Agent;
import com.boxai.domain.agent.AgentRepository;
import com.boxai.infrastructure.persistence.entity.AgentDO;
import com.boxai.infrastructure.persistence.mapper.AgentMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class AgentRepositoryImpl implements AgentRepository {

    private final AgentMapper mapper;

    public AgentRepositoryImpl(AgentMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Agent save(Agent agent) {
        AgentDO row = toDo(agent);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        row.setDeleted(0);
        mapper.insert(row);
        agent.setId(row.getId());
        agent.setCreatedAt(row.getCreatedAt());
        agent.setUpdatedAt(row.getUpdatedAt());
        return agent;
    }

    @Override
    public void update(Agent agent) {
        AgentDO row = toDo(agent);
        row.setId(agent.getId());
        row.setUpdatedAt(LocalDateTime.now());
        mapper.update(row);
        agent.setUpdatedAt(row.getUpdatedAt());
    }

    @Override
    public Optional<Agent> findById(Long id) {
        return Optional.ofNullable(mapper.selectOneById(id)).map(this::toDomain);
    }

    @Override
    public List<Agent> listByWorkspace(Long workspaceId) {
        return mapper.selectListByQuery(
                        QueryWrapper.create().eq("workspace_id", workspaceId).orderBy("updated_at", false))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Agent> searchByName(Long workspaceId, String keyword, int limit) {
        return mapper.selectListByQuery(
                        QueryWrapper.create()
                                .eq("workspace_id", workspaceId)
                                .like("name", keyword)
                                .orderBy("updated_at", false)
                                .limit(limit))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public int countByWorkspace(Long workspaceId) {
        Long count = mapper.selectCountByQuery(QueryWrapper.create().eq("workspace_id", workspaceId));
        return count == null ? 0 : count.intValue();
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    private Agent toDomain(AgentDO row) {
        Agent agent = new Agent();
        agent.setId(row.getId());
        agent.setWorkspaceId(row.getWorkspaceId());
        agent.setName(row.getName());
        agent.setDescription(row.getDescription());
        agent.setAvatarUrl(row.getAvatarUrl());
        agent.setSourceTemplateId(row.getSourceTemplateId());
        agent.setStatus(row.getStatus());
        agent.setPublishedVersionId(row.getPublishedVersionId());
        agent.setCreatedBy(row.getCreatedBy());
        agent.setUpdatedBy(row.getUpdatedBy());
        agent.setCreatedAt(row.getCreatedAt());
        agent.setUpdatedAt(row.getUpdatedAt());
        return agent;
    }

    private AgentDO toDo(Agent agent) {
        AgentDO row = new AgentDO();
        row.setWorkspaceId(agent.getWorkspaceId());
        row.setName(agent.getName());
        row.setDescription(agent.getDescription());
        row.setAvatarUrl(agent.getAvatarUrl());
        row.setSourceTemplateId(agent.getSourceTemplateId());
        row.setStatus(agent.getStatus() == null ? "DRAFT" : agent.getStatus());
        row.setPublishedVersionId(agent.getPublishedVersionId());
        row.setCreatedBy(agent.getCreatedBy());
        row.setUpdatedBy(agent.getUpdatedBy());
        return row;
    }
}
