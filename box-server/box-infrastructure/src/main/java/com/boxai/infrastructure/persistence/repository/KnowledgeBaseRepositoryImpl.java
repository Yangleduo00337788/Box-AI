package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.knowledge.KnowledgeBase;
import com.boxai.domain.knowledge.KnowledgeBaseRepository;
import com.boxai.infrastructure.persistence.entity.KnowledgeBaseDO;
import com.boxai.infrastructure.persistence.mapper.KnowledgeBaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class KnowledgeBaseRepositoryImpl implements KnowledgeBaseRepository {

    private final KnowledgeBaseMapper mapper;

    public KnowledgeBaseRepositoryImpl(KnowledgeBaseMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public KnowledgeBase save(KnowledgeBase knowledgeBase) {
        KnowledgeBaseDO row = toDo(knowledgeBase);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        row.setDeleted(0);
        if (row.getDocumentCount() == null) {
            row.setDocumentCount(0);
        }
        if (row.getChunkCount() == null) {
            row.setChunkCount(0L);
        }
        mapper.insert(row);
        knowledgeBase.setId(row.getId());
        knowledgeBase.setCreatedAt(row.getCreatedAt());
        knowledgeBase.setUpdatedAt(row.getUpdatedAt());
        return knowledgeBase;
    }

    @Override
    public void update(KnowledgeBase knowledgeBase) {
        KnowledgeBaseDO row = toDo(knowledgeBase);
        row.setId(knowledgeBase.getId());
        row.setUpdatedAt(LocalDateTime.now());
        mapper.update(row);
        knowledgeBase.setUpdatedAt(row.getUpdatedAt());
    }

    @Override
    public Optional<KnowledgeBase> findById(Long id) {
        return Optional.ofNullable(mapper.selectOneById(id)).map(this::toDomain);
    }

    @Override
    public List<KnowledgeBase> listByWorkspace(Long workspaceId) {
        return mapper.selectListByQuery(
                        QueryWrapper.create().eq("workspace_id", workspaceId).orderBy("updated_at", false))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    private KnowledgeBase toDomain(KnowledgeBaseDO row) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(row.getId());
        kb.setWorkspaceId(row.getWorkspaceId());
        kb.setName(row.getName());
        kb.setDescription(row.getDescription());
        kb.setIcon(row.getIcon());
        kb.setEmbeddingModelId(row.getEmbeddingModelId());
        kb.setRerankModelId(row.getRerankModelId());
        kb.setChunkConfigJson(row.getChunkConfig());
        kb.setRetrievalConfigJson(row.getRetrievalConfig());
        kb.setDocumentCount(row.getDocumentCount());
        kb.setChunkCount(row.getChunkCount());
        kb.setStatus(row.getStatus());
        kb.setCreatedBy(row.getCreatedBy());
        kb.setCreatedAt(row.getCreatedAt());
        kb.setUpdatedAt(row.getUpdatedAt());
        return kb;
    }

    private KnowledgeBaseDO toDo(KnowledgeBase kb) {
        KnowledgeBaseDO row = new KnowledgeBaseDO();
        row.setWorkspaceId(kb.getWorkspaceId());
        row.setName(kb.getName());
        row.setDescription(kb.getDescription());
        row.setIcon(kb.getIcon());
        row.setEmbeddingModelId(kb.getEmbeddingModelId());
        row.setRerankModelId(kb.getRerankModelId());
        row.setChunkConfig(kb.getChunkConfigJson());
        row.setRetrievalConfig(kb.getRetrievalConfigJson());
        row.setDocumentCount(kb.getDocumentCount());
        row.setChunkCount(kb.getChunkCount());
        row.setStatus(kb.getStatus());
        row.setCreatedBy(kb.getCreatedBy());
        return row;
    }
}
