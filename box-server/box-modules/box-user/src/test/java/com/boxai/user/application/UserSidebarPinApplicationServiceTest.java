package com.boxai.user.application;

import com.boxai.domain.user.UserSidebarPin;
import com.boxai.domain.user.UserSidebarPinRepository;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.user.api.ReplaceSidebarPinsRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSidebarPinApplicationServiceTest {

    @Mock
    private UserSidebarPinRepository userSidebarPinRepository;

    @InjectMocks
    private UserSidebarPinApplicationService service;

    @AfterEach
    void tearDown() {
        WorkspaceContext.clear();
    }

    @Test
    void listSplitsAgentAndConversationPins() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        UserSidebarPin agent = pin("AGENT", 21L);
        UserSidebarPin conversation = pin("CONVERSATION", 88L);
        when(userSidebarPinRepository.listByUserAndWorkspace(3L, 7L)).thenReturn(List.of(agent, conversation));

        var vo = service.list();

        assertEquals(List.of(21L), vo.pinnedAgentIds());
        assertEquals(List.of(88L), vo.pinnedConversationIds());
    }

    @Test
    void replaceTreatsNullListsAsEmpty() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));

        var vo = service.replace(new ReplaceSidebarPinsRequest(null, List.of(9L)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UserSidebarPin>> captor = ArgumentCaptor.forClass(List.class);
        verify(userSidebarPinRepository).replaceAll(eq(3L), eq(7L), captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals("CONVERSATION", captor.getValue().get(0).getPinType());
        assertEquals(9L, captor.getValue().get(0).getTargetId());
        assertEquals(List.of(), vo.pinnedAgentIds());
        assertEquals(List.of(9L), vo.pinnedConversationIds());
    }

    private static UserSidebarPin pin(String type, Long targetId) {
        UserSidebarPin pin = new UserSidebarPin();
        pin.setPinType(type);
        pin.setTargetId(targetId);
        return pin;
    }
}
