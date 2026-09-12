package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.agent.AgentTool;
import com.boxai.domain.agent.AgentToolRepository;
import com.boxai.infrastructure.persistence.entity.AgentToolDO;
import com.boxai.infrastructure.persistence.mapper.AgentToolMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class AgentToolRepositoryImpl implements AgentToolRepository {

    private final AgentToolMapper mapper;

    public AgentToolRepositoryImpl(AgentToolMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public AgentTool save(AgentTool binding) {
        AgentToolDO row = toDo(binding);
        row.setCreatedAt(LocalDateTime.now());
        mapper.insert(row);
        binding.setId(row.getId());
        binding.setCreatedAt(row.getCreatedAt());
        return binding;
    }

    @Override
    public void update(AgentTool binding) {
        AgentToolDO row = toDo(binding);
        row.setId(binding.getId());
        mapper.update(row);
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public Optional<AgentTool> findByVersionAndTool(Long versionId, Long toolId) {
        return Optional.ofNullable(
                        mapper.selectOneByQuery(
                                QueryWrapper.create().eq("version_id", versionId).eq("tool_id", toolId)))
                .map(this::toDomain);
    }

    @Override
    public List<AgentTool> listByVersionId(Long versionId) {
        return mapper.selectListByQuery(QueryWrapper.create().eq("version_id", versionId))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public int countByToolId(Long toolId) {
        Long count = mapper.selectCountByQuery(QueryWrapper.create().eq("tool_id", toolId));
        return count == null ? 0 : count.intValue();
    }

    @Override
    public List<Long> listDistinctAgentIdsByToolId(Long toolId) {
        return mapper.selectListByQuery(QueryWrapper.create().eq("tool_id", toolId))
                .stream()
                .map(AgentToolDO::getAgentId)
                .distinct()
                .toList();
    }

    private AgentTool toDomain(AgentToolDO row) {
        AgentTool binding = new AgentTool();
        binding.setId(row.getId());
        binding.setAgentId(row.getAgentId());
        binding.setVersionId(row.getVersionId());
        binding.setToolId(row.getToolId());
        binding.setEnabled(row.getEnabled() == null || row.getEnabled() == 1);
        binding.setRequireConfirmation(row.getRequireConfirmation() != null && row.getRequireConfirmation() == 1);
        binding.setConfigJson(row.getConfig());
        binding.setCreatedAt(row.getCreatedAt());
        return binding;
    }

    private AgentToolDO toDo(AgentTool binding) {
        AgentToolDO row = new AgentToolDO();
        row.setId(binding.getId());
        row.setAgentId(binding.getAgentId());
        row.setVersionId(binding.getVersionId());
        row.setToolId(binding.getToolId());
        row.setEnabled(Boolean.FALSE.equals(binding.getEnabled()) ? 0 : 1);
        row.setRequireConfirmation(Boolean.TRUE.equals(binding.getRequireConfirmation()) ? 1 : 0);
        row.setConfig(binding.getConfigJson());
        return row;
    }
}
