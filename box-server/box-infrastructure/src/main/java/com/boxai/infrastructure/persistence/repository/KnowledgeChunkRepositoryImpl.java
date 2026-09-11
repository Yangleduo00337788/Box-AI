package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.knowledge.KnowledgeChunk;
import com.boxai.domain.knowledge.KnowledgeChunkRepository;
import com.boxai.infrastructure.persistence.entity.KnowledgeChunkDO;
import com.boxai.infrastructure.persistence.mapper.KnowledgeChunkMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class KnowledgeChunkRepositoryImpl implements KnowledgeChunkRepository {

    private final KnowledgeChunkMapper mapper;

    public KnowledgeChunkRepositoryImpl(KnowledgeChunkMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void saveBatch(List<KnowledgeChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (KnowledgeChunk chunk : chunks) {
            KnowledgeChunkDO row = toDo(chunk);
            row.setCreatedAt(now);
            row.setUpdatedAt(now);
            row.setDeleted(0);
            mapper.insert(row);
            chunk.setId(row.getId());
            chunk.setCreatedAt(row.getCreatedAt());
            chunk.setUpdatedAt(row.getUpdatedAt());
        }
    }

    @Override
    public List<KnowledgeChunk> searchByKeyword(Long knowledgeBaseId, String keyword, int limit) {
        QueryWrapper query = QueryWrapper.create()
                .eq("knowledge_base_id", knowledgeBaseId)
                .like("content", keyword)
                .orderBy("chunk_index", true)
                .limit(Math.max(limit, 1));
        return mapper.selectListByQuery(query).stream().map(this::toDomain).toList();
    }

    @Override
    public List<KnowledgeChunk> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        QueryWrapper query = QueryWrapper.create().in("id", ids);
        List<KnowledgeChunk> chunks = mapper.selectListByQuery(query).stream().map(this::toDomain).toList();
        java.util.Map<Long, KnowledgeChunk> map = new java.util.LinkedHashMap<>();
        for (KnowledgeChunk chunk : chunks) {
            map.put(chunk.getId(), chunk);
        }
        List<KnowledgeChunk> ordered = new ArrayList<>();
        for (Long id : ids) {
            KnowledgeChunk chunk = map.get(id);
            if (chunk != null) {
                ordered.add(chunk);
            }
        }
        return ordered;
    }

    @Override
    public void deleteByDocument(Long documentId) {
        mapper.deleteByQuery(QueryWrapper.create().eq("document_id", documentId));
    }

    @Override
    public int countByKnowledgeBase(Long knowledgeBaseId) {
        Long count = mapper.selectCountByQuery(QueryWrapper.create().eq("knowledge_base_id", knowledgeBaseId));
        return count == null ? 0 : count.intValue();
    }

    private KnowledgeChunk toDomain(KnowledgeChunkDO row) {
        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setId(row.getId());
        chunk.setWorkspaceId(row.getWorkspaceId());
        chunk.setKnowledgeBaseId(row.getKnowledgeBaseId());
        chunk.setDocumentId(row.getDocumentId());
        chunk.setChunkIndex(row.getChunkIndex());
        chunk.setContent(row.getContent());
        chunk.setTokenCount(row.getTokenCount());
        chunk.setPageNumber(row.getPageNumber());
        chunk.setMetadataJson(row.getMetadata());
        chunk.setEsDocumentId(row.getEsDocumentId());
        chunk.setStatus(row.getStatus());
        chunk.setCreatedAt(row.getCreatedAt());
        chunk.setUpdatedAt(row.getUpdatedAt());
        return chunk;
    }

    private KnowledgeChunkDO toDo(KnowledgeChunk chunk) {
        KnowledgeChunkDO row = new KnowledgeChunkDO();
        row.setWorkspaceId(chunk.getWorkspaceId());
        row.setKnowledgeBaseId(chunk.getKnowledgeBaseId());
        row.setDocumentId(chunk.getDocumentId());
        row.setChunkIndex(chunk.getChunkIndex());
        row.setContent(chunk.getContent());
        row.setTokenCount(chunk.getTokenCount());
        row.setPageNumber(chunk.getPageNumber());
        row.setMetadata(chunk.getMetadataJson());
        row.setEsDocumentId(chunk.getEsDocumentId());
        row.setStatus(chunk.getStatus());
        return row;
    }
}
