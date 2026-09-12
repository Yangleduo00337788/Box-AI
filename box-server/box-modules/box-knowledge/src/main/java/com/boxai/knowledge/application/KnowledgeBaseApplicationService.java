package com.boxai.knowledge.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
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

    public KnowledgeBaseApplicationService(KnowledgeBaseRepository knowledgeBaseRepository,
                                           KnowledgeDocumentRepository knowledgeDocumentRepository,
                                           KnowledgeChunkRepository knowledgeChunkRepository,
                                           AgentKnowledgeRepository agentKnowledgeRepository,
                                           WorkspacePermissionService workspacePermissionService,
                                           QuotaApplicationService quotaApplicationService) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.knowledgeChunkRepository = knowledgeChunkRepository;
        this.agentKnowledgeRepository = agentKnowledgeRepository;
        this.workspacePermissionService = workspacePermissionService;
        this.quotaApplicationService = quotaApplicationService;
    }

    public List<KnowledgeBaseVO> list() {
        workspacePermissionService.requirePermission("knowledge:create");
        return knowledgeBaseRepository.listByWorkspace(workspaceId()).stream().map(this::toVO).toList();
    }

    public KnowledgeBaseVO detail(Long id) {
        workspacePermissionService.requirePermission("knowledge:create");
        return toVO(requireKnowledgeBase(id));
    }

    @Transactional
    public KnowledgeBaseVO create(CreateKnowledgeBaseRequest request) {
        workspacePermissionService.requirePermission("knowledge:create");
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
        return toVO(kb);
    }

    @Transactional
    public KnowledgeBaseVO update(Long id, UpdateKnowledgeBaseRequest request) {
        workspacePermissionService.requirePermission("knowledge:create");
        KnowledgeBase kb = requireKnowledgeBase(id);
        kb.setName(request.name().trim());
        kb.setDescription(trimToNull(request.description()));
        kb.setIcon(trimToNull(request.icon()));
        knowledgeBaseRepository.update(kb);
        return toVO(kb);
    }

    @Transactional
    public void delete(Long id) {
        workspacePermissionService.requirePermission("knowledge:delete");
        KnowledgeBase kb = requireKnowledgeBase(id);
        int bindingCount = agentKnowledgeRepository.countByKnowledgeBaseId(kb.getId());
        if (bindingCount > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "该知识库已被 " + bindingCount + " 个智能体绑定，请先解除绑定后再删除");
        }
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
