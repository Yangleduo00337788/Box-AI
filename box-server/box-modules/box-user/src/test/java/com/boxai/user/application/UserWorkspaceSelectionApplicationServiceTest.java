package com.boxai.user.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.agent.Agent;
import com.boxai.domain.agent.AgentRepository;
import com.boxai.domain.user.UserWorkspaceSelection;
import com.boxai.domain.user.UserWorkspaceSelectionRepository;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.user.api.UpdateSidebarSelectionRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserWorkspaceSelectionApplicationServiceTest {

    @Mock
    private UserWorkspaceSelectionRepository userWorkspaceSelectionRepository;
    @Mock
    private AgentRepository agentRepository;

    @InjectMocks
    private UserWorkspaceSelectionApplicationService service;

    @AfterEach
    void tearDown() {
        WorkspaceContext.clear();
    }

    @Test
    void getReturnsNullWhenUnset() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        when(userWorkspaceSelectionRepository.findByUserAndWorkspace(3L, 7L)).thenReturn(Optional.empty());
        assertNull(service.get().selectedAgentId());
    }

    @Test
    void updateRejectsAgentFromOtherWorkspace() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        Agent agent = new Agent();
        agent.setId(21L);
        agent.setWorkspaceId(99L);
        when(agentRepository.findById(21L)).thenReturn(Optional.of(agent));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.update(new UpdateSidebarSelectionRequest(21L)));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    }

    @Test
    void updatePersistsSelection() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        Agent agent = new Agent();
        agent.setId(21L);
        agent.setWorkspaceId(7L);
        when(agentRepository.findById(21L)).thenReturn(Optional.of(agent));
        when(userWorkspaceSelectionRepository.findByUserAndWorkspace(3L, 7L)).thenReturn(Optional.empty());

        var vo = service.update(new UpdateSidebarSelectionRequest(21L));

        ArgumentCaptor<UserWorkspaceSelection> captor = ArgumentCaptor.forClass(UserWorkspaceSelection.class);
        verify(userWorkspaceSelectionRepository).saveOrUpdate(captor.capture());
        assertEquals(3L, captor.getValue().getUserId());
        assertEquals(7L, captor.getValue().getWorkspaceId());
        assertEquals(21L, captor.getValue().getSelectedAgentId());
        assertEquals(21L, vo.selectedAgentId());
    }
}
