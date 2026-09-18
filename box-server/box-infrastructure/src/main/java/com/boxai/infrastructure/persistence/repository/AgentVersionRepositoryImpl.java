package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.agent.AgentVersion;
import com.boxai.domain.agent.AgentVersionRepository;
import com.boxai.infrastructure.persistence.entity.AgentVersionDO;
import com.boxai.infrastructure.persistence.mapper.AgentVersionMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class AgentVersionRepositoryImpl implements AgentVersionRepository {

    private final AgentVersionMapper mapper;

    public AgentVersionRepositoryImpl(AgentVersionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public AgentVersion save(AgentVersion version) {
        AgentVersionDO row = toDo(version);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        row.setDeleted(0);
        mapper.insert(row);
        version.setId(row.getId());
        version.setCreatedAt(row.getCreatedAt());
        version.setUpdatedAt(row.getUpdatedAt());
        return version;
    }

    @Override
    public void update(AgentVersion version) {
        AgentVersionDO row = toDo(version);
        row.setId(version.getId());
        row.setUpdatedAt(LocalDateTime.now());
        mapper.update(row);
        version.setUpdatedAt(row.getUpdatedAt());
    }

    @Override
    public Optional<AgentVersion> findById(Long id) {
        return Optional.ofNullable(mapper.selectOneById(id)).map(this::toDomain);
    }

    @Override
    public Optional<AgentVersion> findLatestDraft(Long agentId) {
        return mapper.selectListByQuery(
                        QueryWrapper.create()
                                .eq("agent_id", agentId)
                                .eq("status", "DRAFT")
                                .orderBy("version_no", false)
                                .limit(1))
                .stream()
                .findFirst()
                .map(this::toDomain);
    }

    @Override
    public Optional<AgentVersion> findByAgentIdAndVersionNo(Long agentId, Integer versionNo) {
        return Optional.ofNullable(
                        mapper.selectOneByQuery(
                                QueryWrapper.create().eq("agent_id", agentId).eq("version_no", versionNo)))
                .map(this::toDomain);
    }

    @Override
    public int maxVersionNo(Long agentId) {
        return mapper.selectListByQuery(
                        QueryWrapper.create().eq("agent_id", agentId).orderBy("version_no", false).limit(1))
                .stream()
                .findFirst()
                .map(AgentVersionDO::getVersionNo)
                .orElse(0);
    }

    @Override
    public List<AgentVersion> listByAgentId(Long agentId) {
        return mapper.selectListByQuery(
                        QueryWrapper.create().eq("agent_id", agentId).orderBy("version_no", false))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private AgentVersion toDomain(AgentVersionDO row) {
        AgentVersion version = new AgentVersion();
        version.setId(row.getId());
        version.setAgentId(row.getAgentId());
        version.setVersionNo(row.getVersionNo());
        version.setVersionName(row.getVersionName());
        version.setStatus(row.getStatus());
        version.setSystemPrompt(row.getSystemPrompt());
        version.setModelId(row.getModelId());
        version.setPlatformModelId(row.getPlatformModelId());
        version.setModelSource(row.getModelSource());
        version.setRoutingPreference(row.getRoutingPreference());
        version.setTemperature(row.getTemperature());
        version.setTopP(row.getTopP());
        version.setMaxTokens(row.getMaxTokens());
        version.setStreamEnabled(row.getStreamEnabled() != null && row.getStreamEnabled() == 1);
        version.setMemoryEnabled(row.getMemoryEnabled() != null && row.getMemoryEnabled() == 1);
        version.setMemoryWindowSize(row.getMemoryWindowSize() == null ? 20 : row.getMemoryWindowSize());
        version.setLongTermMemoryEnabled(row.getLongTermMemoryEnabled() != null && row.getLongTermMemoryEnabled() == 1);
        version.setKnowledgeEnabled(row.getKnowledgeEnabled() != null && row.getKnowledgeEnabled() == 1);
        version.setToolEnabled(row.getToolEnabled() != null && row.getToolEnabled() == 1);
        version.setConfigJson(row.getConfigJson());
        version.setPublishedAt(row.getPublishedAt());
        version.setCreatedBy(row.getCreatedBy());
        version.setUpdatedBy(row.getUpdatedBy());
        version.setCreatedAt(row.getCreatedAt());
        version.setUpdatedAt(row.getUpdatedAt());
        return version;
    }

    private AgentVersionDO toDo(AgentVersion version) {
        AgentVersionDO row = new AgentVersionDO();
        row.setAgentId(version.getAgentId());
        row.setVersionNo(version.getVersionNo());
        row.setVersionName(version.getVersionName());
        row.setStatus(version.getStatus() == null ? "DRAFT" : version.getStatus());
        row.setSystemPrompt(version.getSystemPrompt());
        row.setModelId(version.getModelId());
        row.setPlatformModelId(version.getPlatformModelId());
        row.setModelSource(version.getModelSource());
        row.setRoutingPreference(version.getRoutingPreference() == null ? "BALANCED" : version.getRoutingPreference());
        row.setTemperature(version.getTemperature());
        row.setTopP(version.getTopP());
        row.setMaxTokens(version.getMaxTokens());
        row.setStreamEnabled(Boolean.TRUE.equals(version.getStreamEnabled()) ? 1 : 0);
        row.setMemoryEnabled(Boolean.TRUE.equals(version.getMemoryEnabled()) ? 1 : 0);
        row.setMemoryWindowSize(version.getMemoryWindowSize() == null ? 20 : version.getMemoryWindowSize());
        row.setLongTermMemoryEnabled(Boolean.TRUE.equals(version.getLongTermMemoryEnabled()) ? 1 : 0);
        row.setKnowledgeEnabled(Boolean.TRUE.equals(version.getKnowledgeEnabled()) ? 1 : 0);
        row.setToolEnabled(Boolean.TRUE.equals(version.getToolEnabled()) ? 1 : 0);
        row.setConfigJson(version.getConfigJson());
        row.setPublishedAt(version.getPublishedAt());
        row.setCreatedBy(version.getCreatedBy());
        row.setUpdatedBy(version.getUpdatedBy());
        return row;
    }
}
