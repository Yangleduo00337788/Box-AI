package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.knowledge.KnowledgeDocument;
import com.boxai.domain.knowledge.KnowledgeDocumentRepository;
import com.boxai.infrastructure.persistence.entity.KnowledgeDocumentDO;
import com.boxai.infrastructure.persistence.mapper.KnowledgeDocumentMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class KnowledgeDocumentRepositoryImpl implements KnowledgeDocumentRepository {

    private final KnowledgeDocumentMapper mapper;

    public KnowledgeDocumentRepositoryImpl(KnowledgeDocumentMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public KnowledgeDocument save(KnowledgeDocument document) {
        KnowledgeDocumentDO row = toDo(document);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        row.setDeleted(0);
        mapper.insert(row);
        document.setId(row.getId());
        document.setCreatedAt(row.getCreatedAt());
        document.setUpdatedAt(row.getUpdatedAt());
        return document;
    }

    @Override
    public void update(KnowledgeDocument document) {
        KnowledgeDocumentDO row = toDo(document);
        row.setId(document.getId());
        row.setUpdatedAt(LocalDateTime.now());
        mapper.update(row);
        document.setUpdatedAt(row.getUpdatedAt());
    }

    @Override
    public Optional<KnowledgeDocument> findById(Long id) {
        return Optional.ofNullable(mapper.selectOneById(id)).map(this::toDomain);
    }

    @Override
    public List<KnowledgeDocument> listByKnowledgeBase(Long knowledgeBaseId) {
        return mapper.selectListByQuery(
                        QueryWrapper.create()
                                .eq("knowledge_base_id", knowledgeBaseId)
                                .orderBy("updated_at", false))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    private KnowledgeDocument toDomain(KnowledgeDocumentDO row) {
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setId(row.getId());
        doc.setWorkspaceId(row.getWorkspaceId());
        doc.setKnowledgeBaseId(row.getKnowledgeBaseId());
        doc.setName(row.getName());
        doc.setFileName(row.getFileName());
        doc.setFileType(row.getFileType());
        doc.setMimeType(row.getMimeType());
        doc.setFileSize(row.getFileSize());
        doc.setStorageBucket(row.getStorageBucket());
        doc.setStorageKey(row.getStorageKey());
        doc.setMd5(row.getMd5());
        doc.setPageCount(row.getPageCount());
        doc.setChunkCount(row.getChunkCount());
        doc.setStatus(row.getStatus());
        doc.setProgress(row.getProgress());
        doc.setErrorMessage(row.getErrorMessage());
        doc.setCreatedBy(row.getCreatedBy());
        doc.setCreatedAt(row.getCreatedAt());
        doc.setUpdatedAt(row.getUpdatedAt());
        return doc;
    }

    private KnowledgeDocumentDO toDo(KnowledgeDocument doc) {
        KnowledgeDocumentDO row = new KnowledgeDocumentDO();
        row.setWorkspaceId(doc.getWorkspaceId());
        row.setKnowledgeBaseId(doc.getKnowledgeBaseId());
        row.setName(doc.getName());
        row.setFileName(doc.getFileName());
        row.setFileType(doc.getFileType());
        row.setMimeType(doc.getMimeType());
        row.setFileSize(doc.getFileSize());
        row.setStorageBucket(doc.getStorageBucket());
        row.setStorageKey(doc.getStorageKey());
        row.setMd5(doc.getMd5());
        row.setPageCount(doc.getPageCount());
        row.setChunkCount(doc.getChunkCount());
        row.setStatus(doc.getStatus());
        row.setProgress(doc.getProgress());
        row.setErrorMessage(doc.getErrorMessage() == null ? "" : doc.getErrorMessage());
        row.setCreatedBy(doc.getCreatedBy());
        return row;
    }
}
