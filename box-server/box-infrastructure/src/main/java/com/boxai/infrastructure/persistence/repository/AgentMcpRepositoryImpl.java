package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.agent.AgentMcp;
import com.boxai.domain.agent.AgentMcpRepository;
import com.boxai.infrastructure.persistence.entity.AgentMcpDO;
import com.boxai.infrastructure.persistence.mapper.AgentMcpMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class AgentMcpRepositoryImpl implements AgentMcpRepository {

    private final AgentMcpMapper mapper;

    public AgentMcpRepositoryImpl(AgentMcpMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public AgentMcp save(AgentMcp binding) {
        AgentMcpDO row = toDo(binding);
        row.setCreatedAt(LocalDateTime.now());
        mapper.insert(row);
        binding.setId(row.getId());
        binding.setCreatedAt(row.getCreatedAt());
        return binding;
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public Optional<AgentMcp> findByVersionAndMcpServer(Long versionId, Long mcpServerId) {
        return Optional.ofNullable(
                        mapper.selectOneByQuery(
                                QueryWrapper.create()
                                        .eq("version_id", versionId)
                                        .eq("mcp_server_id", mcpServerId)))
                .map(this::toDomain);
    }

    @Override
    public List<AgentMcp> listByVersionId(Long versionId) {
        return mapper.selectListByQuery(QueryWrapper.create().eq("version_id", versionId))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private AgentMcp toDomain(AgentMcpDO row) {
        AgentMcp binding = new AgentMcp();
        binding.setId(row.getId());
        binding.setAgentId(row.getAgentId());
        binding.setVersionId(row.getVersionId());
        binding.setMcpServerId(row.getMcpServerId());
        binding.setEnabled(row.getEnabled() == null || row.getEnabled() == 1);
        binding.setCreatedAt(row.getCreatedAt());
        return binding;
    }

    private AgentMcpDO toDo(AgentMcp binding) {
        AgentMcpDO row = new AgentMcpDO();
        row.setAgentId(binding.getAgentId());
        row.setVersionId(binding.getVersionId());
        row.setMcpServerId(binding.getMcpServerId());
        row.setEnabled(Boolean.FALSE.equals(binding.getEnabled()) ? 0 : 1);
        return row;
    }
}
