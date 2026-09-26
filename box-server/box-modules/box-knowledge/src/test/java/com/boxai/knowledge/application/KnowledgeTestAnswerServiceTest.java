package com.boxai.knowledge.application;

import com.boxai.ai.ChatModelGateway;
import com.boxai.ai.ModelRuntimeConfig;
import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.knowledge.KnowledgeBase;
import com.boxai.knowledge.api.KnowledgeSearchHitVO;
import com.boxai.knowledge.api.KnowledgeTestAnswerRequest;
import com.boxai.model.application.PlatformModelApplicationService;
import com.boxai.model.platform.ResolvedPlatformModel;
import com.boxai.security.permission.WorkspacePermissionService;
import com.boxai.tenant.application.QuotaApplicationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeTestAnswerServiceTest {

    @Mock
    private KnowledgeBaseApplicationService knowledgeBaseApplicationService;
    @Mock
    private KnowledgeSearchService knowledgeSearchService;
    @Mock
    private ChatModelGateway chatModelGateway;
    @Mock
    private PlatformModelApplicationService platformModelApplicationService;
    @Mock
    private WorkspacePermissionService workspacePermissionService;
    @Mock
    private QuotaApplicationService quotaApplicationService;

    @InjectMocks
    private KnowledgeTestAnswerService service;

    @Test
    void testAnswerReturnsFallbackWhenNoCitations() {
        KnowledgeBase kb = kb();
        when(knowledgeBaseApplicationService.requireKnowledgeBase(4L)).thenReturn(kb);
        when(knowledgeSearchService.searchHits(eq(4L), eq("退款"), eq(5), eq("HYBRID"), org.mockito.ArgumentMatchers.isNull(), eq(false)))
                .thenReturn(List.of());

        var vo = service.testAnswer(4L, new KnowledgeTestAnswerRequest("退款", null));

        verify(workspacePermissionService).requirePermission(PermissionCodes.KNOWLEDGE_READ);
        assertTrue(vo.answer().contains("未检索到"));
        assertTrue(vo.citations().isEmpty());
        verify(chatModelGateway, never()).chat(any(), any(), any(), any(), any(), any());
        verify(quotaApplicationService, never()).consumeAiUsage(anyLong(), anyLong());
    }

    @Test
    void testAnswerRejectsMissingPlatformModel() {
        KnowledgeBase kb = kb();
        when(knowledgeBaseApplicationService.requireKnowledgeBase(4L)).thenReturn(kb);
        when(knowledgeSearchService.searchHits(anyLong(), any(), anyInt(), any(), any(), anyBoolean()))
                .thenReturn(List.of(new KnowledgeSearchHitVO(1L, 2L, 0, "ctx", 0.8)));
        when(platformModelApplicationService.findFirstRunnableModelId()).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.testAnswer(4L, new KnowledgeTestAnswerRequest("退款", 3)));
        assertEquals(ErrorCode.MODEL_NOT_FOUND, ex.getCode());
        verify(quotaApplicationService, never()).consumeAiUsage(anyLong(), anyLong());
    }

    @Test
    void testAnswerChatsWithCitationsAndConsumesQuota() {
        KnowledgeBase kb = kb();
        kb.setRerankModelId(9L);
        when(knowledgeBaseApplicationService.requireKnowledgeBase(4L)).thenReturn(kb);
        when(knowledgeSearchService.searchHits(eq(4L), eq("退款"), eq(3), eq("HYBRID"), org.mockito.ArgumentMatchers.isNull(), eq(true)))
                .thenReturn(List.of(new KnowledgeSearchHitVO(1L, 2L, 0, "政策", 0.8)));
        ModelRuntimeConfig config = new ModelRuntimeConfig("http://llm", "k", "chat");
        when(platformModelApplicationService.findFirstRunnableModelId()).thenReturn(Optional.of(11L));
        when(platformModelApplicationService.resolveForChat(11L)).thenReturn(new ResolvedPlatformModel(config, 1L, 11L));
        when(chatModelGateway.chat(eq(config), any(), any(), eq(0.2), eq(1.0), eq(1024))).thenReturn("根据[1]");

        var vo = service.testAnswer(4L, new KnowledgeTestAnswerRequest("退款", 3));

        verify(quotaApplicationService, times(2)).assertAiQuotaAvailable(7L);
        verify(quotaApplicationService).consumeAiUsage(7L, 2L);
        assertEquals("根据[1]", vo.answer());
        assertEquals(1, vo.citations().size());
    }

    private static KnowledgeBase kb() {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(4L);
        kb.setWorkspaceId(7L);
        return kb;
    }
}
