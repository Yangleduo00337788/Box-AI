package com.boxai.knowledge.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.knowledge.KnowledgeBase;
import com.boxai.domain.knowledge.KnowledgeBaseRepository;
import com.boxai.domain.knowledge.KnowledgeChunkRepository;
import com.boxai.domain.knowledge.KnowledgeDocumentRepository;
import com.boxai.knowledge.api.CreateKnowledgeBaseRequest;
import com.boxai.knowledge.api.KnowledgeBaseVO;
import com.boxai.knowledge.api.UpdateKnowledgeBaseRequest;
import com.boxai.security.context.WorkspaceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class KnowledgeBaseApplicationService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;

    public KnowledgeBaseApplicationService(KnowledgeBaseRepository knowledgeBaseRepository,
                                           KnowledgeDocumentRepository knowledgeDocumentRepository,
                                           KnowledgeChunkRepository knowledgeChunkRepository) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.knowledgeChunkRepository = knowledgeChunkRepository;
    }

    public List<KnowledgeBaseVO> list() {
        return knowledgeBaseRepository.listByWorkspace(workspaceId()).stream().map(this::toVO).toList();
    }

    public KnowledgeBaseVO detail(Long id) {
        return toVO(requireKnowledgeBase(id));
    }

    @Transactional
    public KnowledgeBaseVO create(CreateKnowledgeBaseRequest request) {
        Long userId = WorkspaceContext.require().userId();
        KnowledgeBase kb = new KnowledgeBase();
        kb.setWorkspaceId(workspaceId());
        kb.setName(request.name().trim());
        kb.setDescription(trimToNull(request.description()));
        kb.setIcon(trimToNull(request.icon()));
        kb.setDocumentCount(0);
        kb.setChunkCount(0L);
        kb.setStatus("READY");
        kb.setCreatedBy(userId);
        knowledgeBaseRepository.save(kb);
        return toVO(kb);
    }

    @Transactional
    public KnowledgeBaseVO update(Long id, UpdateKnowledgeBaseRequest request) {
        KnowledgeBase kb = requireKnowledgeBase(id);
        kb.setName(request.name().trim());
        kb.setDescription(trimToNull(request.description()));
        kb.setIcon(trimToNull(request.icon()));
        knowledgeBaseRepository.update(kb);
        return toVO(kb);
    }

    @Transactional
    public void delete(Long id) {
        KnowledgeBase kb = requireKnowledgeBase(id);
        knowledgeDocumentRepository.listByKnowledgeBase(kb.getId()).forEach(doc -> {
            knowledgeChunkRepository.deleteByDocument(doc.getId());
            knowledgeDocumentRepository.delete(doc.getId());
        });
        knowledgeBaseRepository.delete(id);
    }

    KnowledgeBase requireKnowledgeBase(Long id) {
        KnowledgeBase kb = knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.KNOWLEDGE_NOT_FOUND, "知识库不存在"));
        if (!workspaceId().equals(kb.getWorkspaceId())) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "无权访问该知识库");
        }
        return kb;
    }

    private KnowledgeBaseVO toVO(KnowledgeBase kb) {
        return new KnowledgeBaseVO(
                kb.getId(),
                kb.getName(),
                kb.getDescription(),
                kb.getIcon(),
                kb.getDocumentCount(),
                kb.getChunkCount(),
                kb.getStatus(),
                kb.getCreatedAt(),
                kb.getUpdatedAt());
    }

    private Long workspaceId() {
        return WorkspaceContext.require().workspaceId();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
