package com.boxai.user.application;

import com.boxai.domain.user.UserSidebarPin;
import com.boxai.domain.user.UserSidebarPinRepository;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.user.api.ReplaceSidebarPinsRequest;
import com.boxai.user.api.SidebarPinsVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserSidebarPinApplicationService {

    private static final String PIN_TYPE_AGENT = "AGENT";
    private static final String PIN_TYPE_CONVERSATION = "CONVERSATION";

    private final UserSidebarPinRepository userSidebarPinRepository;

    public UserSidebarPinApplicationService(UserSidebarPinRepository userSidebarPinRepository) {
        this.userSidebarPinRepository = userSidebarPinRepository;
    }

    public SidebarPinsVO list() {
        Long userId = WorkspaceContext.require().userId();
        Long workspaceId = WorkspaceContext.require().workspaceId();
        List<UserSidebarPin> pins = userSidebarPinRepository.listByUserAndWorkspace(userId, workspaceId);
        List<Long> agentIds = new ArrayList<>();
        List<Long> conversationIds = new ArrayList<>();
        for (UserSidebarPin pin : pins) {
            if (PIN_TYPE_AGENT.equals(pin.getPinType())) {
                agentIds.add(pin.getTargetId());
            } else if (PIN_TYPE_CONVERSATION.equals(pin.getPinType())) {
                conversationIds.add(pin.getTargetId());
            }
        }
        return new SidebarPinsVO(agentIds, conversationIds);
    }

    @Transactional
    public SidebarPinsVO replace(ReplaceSidebarPinsRequest request) {
        Long userId = WorkspaceContext.require().userId();
        Long workspaceId = WorkspaceContext.require().workspaceId();
        List<UserSidebarPin> pins = new ArrayList<>();
        List<Long> agentIds = request.pinnedAgentIds() == null ? List.of() : request.pinnedAgentIds();
        List<Long> conversationIds = request.pinnedConversationIds() == null ? List.of() : request.pinnedConversationIds();
        for (int i = 0; i < agentIds.size(); i++) {
            UserSidebarPin pin = new UserSidebarPin();
            pin.setPinType(PIN_TYPE_AGENT);
            pin.setTargetId(agentIds.get(i));
            pin.setSortOrder(i);
            pins.add(pin);
        }
        for (int i = 0; i < conversationIds.size(); i++) {
            UserSidebarPin pin = new UserSidebarPin();
            pin.setPinType(PIN_TYPE_CONVERSATION);
            pin.setTargetId(conversationIds.get(i));
            pin.setSortOrder(i);
            pins.add(pin);
        }
        userSidebarPinRepository.replaceAll(userId, workspaceId, pins);
        return new SidebarPinsVO(agentIds, conversationIds);
    }
}
