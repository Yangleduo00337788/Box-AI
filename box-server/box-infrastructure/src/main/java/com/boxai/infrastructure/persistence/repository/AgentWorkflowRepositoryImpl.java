package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.agent.AgentWorkflow;
import com.boxai.domain.agent.AgentWorkflowRepository;
import com.boxai.infrastructure.persistence.entity.AgentWorkflowDO;
import com.boxai.infrastructure.persistence.mapper.AgentWorkflowMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class AgentWorkflowRepositoryImpl implements AgentWorkflowRepository {

    private final AgentWorkflowMapper mapper;

    public AgentWorkflowRepositoryImpl(AgentWorkflowMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public AgentWorkflow save(AgentWorkflow binding) {
        AgentWorkflowDO row = toDo(binding);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        mapper.insert(row);
        binding.setId(row.getId());
        binding.setCreatedAt(row.getCreatedAt());
        binding.setUpdatedAt(row.getUpdatedAt());
        return binding;
    }

    @Override
    public void update(AgentWorkflow binding) {
        AgentWorkflowDO row = toDo(binding);
        row.setUpdatedAt(LocalDateTime.now());
        mapper.update(row);
        binding.setUpdatedAt(row.getUpdatedAt());
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public Optional<AgentWorkflow> findByVersionAndWorkflow(Long versionId, Long workflowId) {
        return Optional.ofNullable(mapper.selectOneByQuery(
                        QueryWrapper.create()
                                .eq("version_id", versionId)
                                .eq("workflow_id", workflowId)))
                .map(this::toDomain);
    }

    @Override
    public List<AgentWorkflow> listByVersionId(Long versionId) {
        return mapper.selectListByQuery(
                        QueryWrapper.create()
                                .eq("version_id", versionId)
                                .orderBy("id", true))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<AgentWorkflow> findDefaultByVersionId(Long versionId) {
        return Optional.ofNullable(mapper.selectOneByQuery(
                        QueryWrapper.create()
                                .eq("version_id", versionId)
                                .eq("is_default", 1)
                                .eq("enabled", 1)))
                .map(this::toDomain);
    }

    @Override
    public void clearDefaultForVersion(Long versionId) {
        List<AgentWorkflowDO> rows = mapper.selectListByQuery(
                QueryWrapper.create().eq("version_id", versionId).eq("is_default", 1));
        for (AgentWorkflowDO row : rows) {
            row.setDefaultWorkflow(0);
            row.setUpdatedAt(LocalDateTime.now());
            mapper.update(row);
        }
    }

    private AgentWorkflow toDomain(AgentWorkflowDO row) {
        AgentWorkflow binding = new AgentWorkflow();
        binding.setId(row.getId());
        binding.setAgentId(row.getAgentId());
        binding.setVersionId(row.getVersionId());
        binding.setWorkflowId(row.getWorkflowId());
        binding.setEnabled(row.getEnabled() == null || row.getEnabled() != 0);
        binding.setDefaultWorkflow(row.getDefaultWorkflow() != null && row.getDefaultWorkflow() != 0);
        binding.setCallable(row.getCallable() == null || row.getCallable() != 0);
        binding.setCreatedAt(row.getCreatedAt());
        binding.setUpdatedAt(row.getUpdatedAt());
        return binding;
    }

    private AgentWorkflowDO toDo(AgentWorkflow binding) {
        AgentWorkflowDO row = new AgentWorkflowDO();
        row.setId(binding.getId());
        row.setAgentId(binding.getAgentId());
        row.setVersionId(binding.getVersionId());
        row.setWorkflowId(binding.getWorkflowId());
        row.setEnabled(Boolean.FALSE.equals(binding.getEnabled()) ? 0 : 1);
        row.setDefaultWorkflow(Boolean.TRUE.equals(binding.getDefaultWorkflow()) ? 1 : 0);
        row.setCallable(Boolean.FALSE.equals(binding.getCallable()) ? 0 : 1);
        return row;
    }
}
