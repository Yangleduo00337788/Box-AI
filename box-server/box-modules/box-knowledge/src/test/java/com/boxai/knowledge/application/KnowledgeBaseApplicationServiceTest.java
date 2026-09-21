package com.boxai.knowledge.application;

import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.knowledge.KnowledgeBase;
import com.boxai.domain.knowledge.KnowledgeBaseRepository;
import com.boxai.domain.knowledge.KnowledgeChunkRepository;
import com.boxai.domain.knowledge.KnowledgeDocumentRepository;
import com.boxai.domain.agent.AgentKnowledgeRepository;
import com.boxai.knowledge.api.CreateKnowledgeBaseRequest;
import com.boxai.security.audit.AuditLogService;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.guard.ResourceDeleteGuard;
import com.boxai.security.permission.WorkspacePermissionService;
import com.boxai.tenant.application.QuotaApplicationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeBaseApplicationServiceTest {

    @Mock
    private KnowledgeBaseRepository knowledgeBaseRepository;
    @Mock
    private KnowledgeDocumentRepository knowledgeDocumentRepository;
    @Mock
    private KnowledgeChunkRepository knowledgeChunkRepository;
    @Mock
    private AgentKnowledgeRepository agentKnowledgeRepository;
    @Mock
    private WorkspacePermissionService workspacePermissionService;
    @Mock
    private QuotaApplicationService quotaApplicationService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private ResourceDeleteGuard resourceDeleteGuard;

    @InjectMocks
    private KnowledgeBaseApplicationService service;

    @AfterEach
    void tearDown() {
        WorkspaceContext.clear();
    }

    @Test
    void createChecksPermissionQuotaAndSaves() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        CreateKnowledgeBaseRequest request = new CreateKnowledgeBaseRequest("KB", "desc", null);

        ArgumentCaptor<KnowledgeBase> captor = ArgumentCaptor.forClass(KnowledgeBase.class);
        service.create(request);

        verify(workspacePermissionService).requirePermission(PermissionCodes.KNOWLEDGE_CREATE);
        verify(quotaApplicationService).assertKnowledgeBaseQuotaAvailable(7L);
        verify(knowledgeBaseRepository).save(captor.capture());
        assertEquals("KB", captor.getValue().getName());
        assertEquals(7L, captor.getValue().getWorkspaceId());
        verify(auditLogService).recordSuccess(
                org.mockito.ArgumentMatchers.eq(com.boxai.common.constant.AuditActions.KNOWLEDGE_CREATE),
                org.mockito.ArgumentMatchers.eq(com.boxai.common.constant.AuditResourceTypes.KNOWLEDGE),
                org.mockito.ArgumentMatchers.nullable(Long.class),
                org.mockito.ArgumentMatchers.eq("KB"),
                org.mockito.ArgumentMatchers.isNull());
    }

    @Test
    void detailRejectsCrossWorkspaceKnowledgeBase() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(1L);
        kb.setWorkspaceId(99L);
        kb.setName("other");
        when(knowledgeBaseRepository.findById(1L)).thenReturn(Optional.of(kb));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.detail(1L));
        assertEquals(ErrorCode.WORKSPACE_ACCESS_DENIED, ex.getCode());
        verify(workspacePermissionService).requirePermission(PermissionCodes.KNOWLEDGE_READ);
    }
}
