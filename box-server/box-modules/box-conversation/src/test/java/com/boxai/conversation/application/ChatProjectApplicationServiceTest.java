package com.boxai.conversation.application;

import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.exception.BusinessException;
import com.boxai.conversation.api.CreateChatProjectRequest;
import com.boxai.domain.conversation.ChatProject;
import com.boxai.domain.conversation.ChatProjectRepository;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.permission.WorkspacePermissionService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatProjectApplicationServiceTest {

    @Mock
    private ChatProjectRepository chatProjectRepository;

    @Mock
    private WorkspacePermissionService workspacePermissionService;

    @InjectMocks
    private ChatProjectApplicationService service;

    @AfterEach
    void tearDown() {
        WorkspaceContext.clear();
    }

    @Test
    void createRequiresAgentReadPermissionAndPersistsProject() {
        WorkspaceContext.set(new WorkspaceContext(10L, 20L, 1L, "MEMBER"));
        CreateChatProjectRequest request = new CreateChatProjectRequest("  Demo  ");

        when(chatProjectRepository.countConversations(null)).thenReturn(0);

        ArgumentCaptor<ChatProject> captor = ArgumentCaptor.forClass(ChatProject.class);
        service.create(request);

        verify(workspacePermissionService).requirePermission(PermissionCodes.AGENT_READ);
        verify(chatProjectRepository).save(captor.capture());
        ChatProject saved = captor.getValue();
        assertEquals(10L, saved.getWorkspaceId());
        assertEquals(20L, saved.getUserId());
        assertEquals("Demo", saved.getName());
    }

    @Test
    void deleteRejectsWhenProjectNotOwned() {
        WorkspaceContext.set(new WorkspaceContext(10L, 20L, 1L, "MEMBER"));
        ChatProject project = new ChatProject();
        project.setId(5L);
        project.setWorkspaceId(10L);
        project.setUserId(99L);
        when(chatProjectRepository.findById(5L)).thenReturn(Optional.of(project));

        assertThrows(BusinessException.class, () -> service.delete(5L));
        verify(workspacePermissionService, org.mockito.Mockito.times(2))
                .requirePermission(PermissionCodes.AGENT_READ);
    }

    @Test
    void listPropagatesPermissionFailure() {
        WorkspaceContext.set(new WorkspaceContext(10L, 20L, 1L, "MEMBER"));
        doThrow(new BusinessException(com.boxai.common.exception.ErrorCode.FORBIDDEN, "无权访问"))
                .when(workspacePermissionService)
                .requirePermission(PermissionCodes.AGENT_READ);

        assertThrows(BusinessException.class, () -> service.list());
    }
}
