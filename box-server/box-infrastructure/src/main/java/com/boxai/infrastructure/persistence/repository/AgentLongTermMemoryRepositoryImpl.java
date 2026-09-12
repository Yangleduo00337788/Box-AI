package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.agent.AgentLongTermMemory;
import com.boxai.domain.agent.AgentLongTermMemoryRepository;
import com.boxai.infrastructure.persistence.entity.AgentLongTermMemoryDO;
import com.boxai.infrastructure.persistence.mapper.AgentLongTermMemoryMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AgentLongTermMemoryRepositoryImpl implements AgentLongTermMemoryRepository {

    private final AgentLongTermMemoryMapper mapper;

    public AgentLongTermMemoryRepositoryImpl(AgentLongTermMemoryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<AgentLongTermMemory> findById(Long id) {
        return Optional.ofNullable(mapper.selectOneById(id)).map(this::toDomain);
    }

    @Override
    public List<AgentLongTermMemory> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return mapper.selectListByQuery(QueryWrapper.create().in("id", ids)).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<AgentLongTermMemory> listByAgentAndUser(Long agentId, Long userId) {
        if (agentId == null || userId == null) {
            return List.of();
        }
        return mapper.selectListByQuery(QueryWrapper.create()
                        .eq("agent_id", agentId)
                        .eq("user_id", userId)
                        .orderBy("created_at", false))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public AgentLongTermMemory save(AgentLongTermMemory memory) {
        AgentLongTermMemoryDO row = toDo(memory);
        if (row.getId() == null) {
            mapper.insert(row);
            memory.setId(row.getId());
            memory.setCreatedAt(row.getCreatedAt());
            memory.setUpdatedAt(row.getUpdatedAt());
        } else {
            mapper.update(row);
        }
        return memory;
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    private AgentLongTermMemory toDomain(AgentLongTermMemoryDO row) {
        AgentLongTermMemory memory = new AgentLongTermMemory();
        memory.setId(row.getId());
        memory.setAgentId(row.getAgentId());
        memory.setWorkspaceId(row.getWorkspaceId());
        memory.setUserId(row.getUserId());
        memory.setContent(row.getContent());
        memory.setEsDocumentId(row.getEsDocumentId());
        memory.setCreatedAt(row.getCreatedAt());
        memory.setUpdatedAt(row.getUpdatedAt());
        return memory;
    }

    private AgentLongTermMemoryDO toDo(AgentLongTermMemory memory) {
        AgentLongTermMemoryDO row = new AgentLongTermMemoryDO();
        row.setId(memory.getId());
        row.setAgentId(memory.getAgentId());
        row.setWorkspaceId(memory.getWorkspaceId());
        row.setUserId(memory.getUserId());
        row.setContent(memory.getContent());
        row.setEsDocumentId(memory.getEsDocumentId());
        return row;
    }
}
