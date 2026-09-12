package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.agent.AgentKnowledge;
import com.boxai.domain.agent.AgentKnowledgeRepository;
import com.boxai.infrastructure.persistence.entity.AgentKnowledgeDO;
import com.boxai.infrastructure.persistence.mapper.AgentKnowledgeMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class AgentKnowledgeRepositoryImpl implements AgentKnowledgeRepository {

    private final AgentKnowledgeMapper mapper;

    public AgentKnowledgeRepositoryImpl(AgentKnowledgeMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public AgentKnowledge save(AgentKnowledge binding) {
        AgentKnowledgeDO row = toDo(binding);
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
    public Optional<AgentKnowledge> findByVersionAndKnowledgeBase(Long versionId, Long knowledgeBaseId) {
        return Optional.ofNullable(
                        mapper.selectOneByQuery(
                                QueryWrapper.create()
                                        .eq("version_id", versionId)
                                        .eq("knowledge_base_id", knowledgeBaseId)))
                .map(this::toDomain);
    }

    @Override
    public List<AgentKnowledge> listByVersionId(Long versionId) {
        return mapper.selectListByQuery(QueryWrapper.create().eq("version_id", versionId))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public int countByKnowledgeBaseId(Long knowledgeBaseId) {
        Long count = mapper.selectCountByQuery(QueryWrapper.create().eq("knowledge_base_id", knowledgeBaseId));
        return count == null ? 0 : count.intValue();
    }

    @Override
    public List<Long> listDistinctAgentIdsByKnowledgeBaseId(Long knowledgeBaseId) {
        return mapper.selectListByQuery(QueryWrapper.create().eq("knowledge_base_id", knowledgeBaseId))
                .stream()
                .map(AgentKnowledgeDO::getAgentId)
                .distinct()
                .toList();
    }

    private AgentKnowledge toDomain(AgentKnowledgeDO row) {
        AgentKnowledge binding = new AgentKnowledge();
        binding.setId(row.getId());
        binding.setAgentId(row.getAgentId());
        binding.setVersionId(row.getVersionId());
        binding.setKnowledgeBaseId(row.getKnowledgeBaseId());
        binding.setTopK(row.getTopK());
        binding.setScoreThreshold(row.getScoreThreshold());
        binding.setRetrievalMode(row.getRetrievalMode());
        binding.setRerankEnabled(row.getRerankEnabled() != null && row.getRerankEnabled() == 1);
        binding.setCitationEnabled(row.getCitationEnabled() != null && row.getCitationEnabled() == 1);
        binding.setCreatedAt(row.getCreatedAt());
        return binding;
    }

    private AgentKnowledgeDO toDo(AgentKnowledge binding) {
        AgentKnowledgeDO row = new AgentKnowledgeDO();
        row.setAgentId(binding.getAgentId());
        row.setVersionId(binding.getVersionId());
        row.setKnowledgeBaseId(binding.getKnowledgeBaseId());
        row.setTopK(binding.getTopK() == null ? 5 : binding.getTopK());
        row.setScoreThreshold(binding.getScoreThreshold());
        row.setRetrievalMode(binding.getRetrievalMode() == null ? "HYBRID" : binding.getRetrievalMode());
        row.setRerankEnabled(Boolean.FALSE.equals(binding.getRerankEnabled()) ? 0 : 1);
        row.setCitationEnabled(Boolean.FALSE.equals(binding.getCitationEnabled()) ? 0 : 1);
        return row;
    }
}
