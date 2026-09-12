package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.agent.AgentSubAgent;
import com.boxai.domain.agent.AgentSubAgentRepository;
import com.boxai.infrastructure.persistence.entity.AgentSubAgentDO;
import com.boxai.infrastructure.persistence.mapper.AgentSubAgentMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class AgentSubAgentRepositoryImpl implements AgentSubAgentRepository {

    private final AgentSubAgentMapper mapper;

    public AgentSubAgentRepositoryImpl(AgentSubAgentMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public AgentSubAgent save(AgentSubAgent binding) {
        AgentSubAgentDO row = toDo(binding);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        mapper.insert(row);
        binding.setId(row.getId());
        binding.setCreatedAt(row.getCreatedAt());
        binding.setUpdatedAt(row.getUpdatedAt());
        return binding;
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public Optional<AgentSubAgent> findByVersionAndSubAgent(Long versionId, Long subAgentId) {
        return Optional.ofNullable(mapper.selectOneByQuery(
                        QueryWrapper.create()
                                .eq("version_id", versionId)
                                .eq("sub_agent_id", subAgentId)))
                .map(this::toDomain);
    }

    @Override
    public List<AgentSubAgent> listByVersionId(Long versionId) {
        return mapper.selectListByQuery(
                        QueryWrapper.create()
                                .eq("version_id", versionId)
                                .orderBy("sort_order", true)
                                .orderBy("id", true))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public int countBySubAgentId(Long subAgentId) {
        Long count = mapper.selectCountByQuery(QueryWrapper.create().eq("sub_agent_id", subAgentId));
        return count == null ? 0 : count.intValue();
    }

    private AgentSubAgent toDomain(AgentSubAgentDO row) {
        AgentSubAgent binding = new AgentSubAgent();
        binding.setId(row.getId());
        binding.setAgentId(row.getAgentId());
        binding.setVersionId(row.getVersionId());
        binding.setSubAgentId(row.getSubAgentId());
        binding.setEnabled(row.getEnabled() != null && row.getEnabled() == 1);
        binding.setSortOrder(row.getSortOrder());
        binding.setCreatedAt(row.getCreatedAt());
        binding.setUpdatedAt(row.getUpdatedAt());
        return binding;
    }

    private AgentSubAgentDO toDo(AgentSubAgent binding) {
        AgentSubAgentDO row = new AgentSubAgentDO();
        row.setAgentId(binding.getAgentId());
        row.setVersionId(binding.getVersionId());
        row.setSubAgentId(binding.getSubAgentId());
        row.setEnabled(Boolean.FALSE.equals(binding.getEnabled()) ? 0 : 1);
        row.setSortOrder(binding.getSortOrder() == null ? 0 : binding.getSortOrder());
        return row;
    }
}
