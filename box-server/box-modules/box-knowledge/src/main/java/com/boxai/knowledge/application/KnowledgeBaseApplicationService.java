package com.boxai.knowledge.application;

import com.boxai.common.constant.AuditActions;
import com.boxai.common.constant.AuditResourceTypes;
import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.security.audit.AuditLogService;
import com.boxai.security.guard.ResourceDeleteGuard;
import com.boxai.domain.agent.AgentKnowledgeRepository;
import com.boxai.domain.knowledge.KnowledgeBase;
import com.boxai.domain.knowledge.KnowledgeBaseRepository;
import com.boxai.domain.knowledge.KnowledgeChunkRepository;
import com.boxai.domain.knowledge.KnowledgeDocumentRepository;
import com.boxai.knowledge.api.CreateKnowledgeBaseRequest;
import com.boxai.knowledge.api.KnowledgeBaseVO;
import com.boxai.knowledge.api.UpdateKnowledgeBaseRequest;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.permission.WorkspacePermissionService;
import com.boxai.tenant.application.QuotaApplicationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class KnowledgeBaseApplicationService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final AgentKnowledgeRepository agentKnowledgeRepository;
    private final WorkspacePermissionService workspacePermissionService;
    private final QuotaApplicationService quotaApplicationService;
    private final AuditLogService auditLogService;
    private final ResourceDeleteGuard resourceDeleteGuard;

    public KnowledgeBaseApplicationService(KnowledgeBaseRepository knowledgeBaseRepository,
                                           KnowledgeDocumentRepository knowledgeDocumentRepository,
                                           KnowledgeChunkRepository knowledgeChunkRepository,
                                           AgentKnowledgeRepository agentKnowledgeRepository,
                                           WorkspacePermissionService workspacePermissionService,
                                           QuotaApplicationService quotaApplicationService,
                                           AuditLogService auditLogService,
                                           ResourceDeleteGuard resourceDeleteGuard) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.knowledgeChunkRepository = knowledgeChunkRepository;
        this.agentKnowledgeRepository = agentKnowledgeRepository;
        this.workspacePermissionService = workspacePermissionService;
        this.quotaApplicationService = quotaApplicationService;
        this.auditLogService = auditLogService;
        this.resourceDeleteGuard = resourceDeleteGuard;
    }

    public List<KnowledgeBaseVO> list() {
        workspacePermissionService.requirePermission(PermissionCodes.KNOWLEDGE_READ);
        return knowledgeBaseRepository.listByWorkspace(workspaceId()).stream().map(this::toVO).toList();
    }

    public KnowledgeBaseVO detail(Long id) {
        workspacePermissionService.requirePermission(PermissionCodes.KNOWLEDGE_READ);
        return toVO(requireKnowledgeBase(id));
    }

    @Transactional
    public KnowledgeBaseVO create(CreateKnowledgeBaseRequest request) {
        workspacePermissionService.requirePermission(PermissionCodes.KNOWLEDGE_CREATE);
        Long workspaceId = workspaceId();
        quotaApplicationService.assertKnowledgeBaseQuotaAvailable(workspaceId);
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
        auditLogService.recordSuccess(
                AuditActions.KNOWLEDGE_CREATE,
                AuditResourceTypes.KNOWLEDGE,
                kb.getId(),
                kb.getName(),
                null);
        return toVO(kb);
    }

    @Transactional
    public KnowledgeBaseVO update(Long id, UpdateKnowledgeBaseRequest request) {
        workspacePermissionService.requirePermission(PermissionCodes.KNOWLEDGE_UPDATE);
        KnowledgeBase kb = requireKnowledgeBase(id);
        kb.setName(request.name().trim());
        kb.setDescription(trimToNull(request.description()));
        kb.setIcon(trimToNull(request.icon()));
        kb.setEmbeddingModelId(request.embeddingModelId());
        kb.setRerankModelId(request.rerankModelId());
        knowledgeBaseRepository.update(kb);
        return toVO(kb);
    }

    @Transactional
    public void delete(Long id) {
        workspacePermissionService.requirePermission(PermissionCodes.KNOWLEDGE_DELETE);
        KnowledgeBase kb = requireKnowledgeBase(id);
        resourceDeleteGuard.assertKnowledgeDeletable(kb);
        knowledgeDocumentRepository.listByKnowledgeBase(kb.getId()).forEach(doc -> {
            knowledgeChunkRepository.deleteByDocument(doc.getId());
            knowledgeDocumentRepository.delete(doc.getId());
        });
        knowledgeBaseRepository.delete(id);
        auditLogService.recordSuccess(
                AuditActions.KNOWLEDGE_DELETE,
                AuditResourceTypes.KNOWLEDGE,
                id,
                kb.getName(),
                null);
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
                kb.getEmbeddingModelId(),
                kb.getRerankModelId(),
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
