package com.boxai.agent.application;

import com.boxai.ai.ChatModelGateway;
import com.boxai.ai.EmbeddingModelGateway;
import com.boxai.ai.ModelRuntimeConfig;
import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.agent.Agent;
import com.boxai.domain.agent.AgentLongTermMemory;
import com.boxai.domain.agent.AgentLongTermMemoryRepository;
import com.boxai.domain.agent.AgentLongTermMemorySearchIndex;
import com.boxai.domain.agent.AgentRepository;
import com.boxai.domain.agent.AgentVersion;
import com.boxai.model.application.PlatformModelApplicationService;
import com.boxai.model.platform.ResolvedPlatformModel;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.permission.WorkspacePermissionService;
import com.boxai.tenant.application.QuotaApplicationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentLongTermMemoryApplicationServiceTest {

    @Mock
    private AgentRepository agentRepository;
    @Mock
    private AgentLongTermMemoryRepository memoryRepository;
    @Mock
    private AgentLongTermMemorySearchIndex searchIndex;
    @Mock
    private EmbeddingModelGateway embeddingModelGateway;
    @Mock
    private ChatModelGateway chatModelGateway;
    @Mock
    private PlatformModelApplicationService platformModelApplicationService;
    @Mock
    private QuotaApplicationService quotaApplicationService;
    @Mock
    private WorkspacePermissionService workspacePermissionService;

    private AgentLongTermMemoryApplicationService service;

    @BeforeEach
    void setUp() {
        service = new AgentLongTermMemoryApplicationService(
                agentRepository,
                memoryRepository,
                searchIndex,
                embeddingModelGateway,
                chatModelGateway,
                platformModelApplicationService,
                quotaApplicationService,
                workspacePermissionService,
                "text-embedding-3-small");
    }

    @AfterEach
    void tearDown() {
        WorkspaceContext.clear();
    }

    @Test
    void buildContextReturnsEmptyWhenDisabled() {
        AgentVersion version = new AgentVersion();
        version.setLongTermMemoryEnabled(false);
        assertEquals("", service.buildContext(version, 21L, "我喜欢茶叶"));
        verify(searchIndex, never()).searchByVector(anyLong(), anyLong(), any(), anyInt());
    }

    @Test
    void captureFromTurnSkipsWhenModelReturnsNone() {
        AgentVersion version = new AgentVersion();
        version.setLongTermMemoryEnabled(true);
        ModelRuntimeConfig config = new ModelRuntimeConfig("http://llm", "k", "chat");
        when(platformModelApplicationService.findFirstRunnableModelId()).thenReturn(Optional.of(11L));
        when(platformModelApplicationService.resolveForChat(11L)).thenReturn(new ResolvedPlatformModel(config, 1L, 11L));
        when(chatModelGateway.chat(any(), any(), any(), any(), any(), any())).thenReturn("NONE");

        service.captureFromTurn(version, 21L, 7L, 3L, "我叫 Ada", "你好 Ada");

        verify(quotaApplicationService).consumeAiUsage(anyLong(), anyLong());
        verify(memoryRepository, never()).save(any());
    }

    @Test
    void listMapsMemoriesForCurrentUser() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        when(agentRepository.findById(21L)).thenReturn(Optional.of(agent()));
        AgentLongTermMemory memory = new AgentLongTermMemory();
        memory.setId(4L);
        memory.setContent("喜欢绿茶");
        when(memoryRepository.listByAgentAndUser(21L, 3L)).thenReturn(List.of(memory));

        var vos = service.list(21L);

        verify(workspacePermissionService).requirePermission(PermissionCodes.AGENT_READ);
        assertEquals("喜欢绿茶", vos.get(0).content());
    }

    @Test
    void deleteRejectsMemoryOwnedByAnotherUser() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        when(agentRepository.findById(21L)).thenReturn(Optional.of(agent()));
        AgentLongTermMemory memory = new AgentLongTermMemory();
        memory.setId(4L);
        memory.setAgentId(21L);
        memory.setUserId(99L);
        when(memoryRepository.findById(4L)).thenReturn(Optional.of(memory));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete(21L, 4L));
        assertEquals(ErrorCode.WORKSPACE_ACCESS_DENIED, ex.getCode());
        verify(memoryRepository, never()).delete(anyLong());
    }

    @Test
    void deleteRemovesIndexAndRow() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        when(agentRepository.findById(21L)).thenReturn(Optional.of(agent()));
        AgentLongTermMemory memory = new AgentLongTermMemory();
        memory.setId(4L);
        memory.setAgentId(21L);
        memory.setUserId(3L);
        memory.setEsDocumentId("es-4");
        when(memoryRepository.findById(4L)).thenReturn(Optional.of(memory));

        service.delete(21L, 4L);

        verify(searchIndex).deleteMemory("es-4");
        verify(memoryRepository).delete(4L);
    }

    @Test
    void captureFromTurnNoopsWhenDisabled() {
        AgentVersion version = new AgentVersion();
        version.setLongTermMemoryEnabled(false);
        service.captureFromTurn(version, 21L, 7L, 3L, "hi", "hello");
        verify(chatModelGateway, never()).chat(any(), any(), any(), any(), any(), any());
    }

    private static Agent agent() {
        Agent agent = new Agent();
        agent.setId(21L);
        agent.setWorkspaceId(7L);
        return agent;
    }
}
