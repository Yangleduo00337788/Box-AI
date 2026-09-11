package com.boxai.user.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.agent.Agent;
import com.boxai.domain.agent.AgentRepository;
import com.boxai.domain.user.UserWorkspaceSelection;
import com.boxai.domain.user.UserWorkspaceSelectionRepository;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.user.api.SidebarSelectionVO;
import com.boxai.user.api.UpdateSidebarSelectionRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserWorkspaceSelectionApplicationService {

    private final UserWorkspaceSelectionRepository userWorkspaceSelectionRepository;
    private final AgentRepository agentRepository;

    public UserWorkspaceSelectionApplicationService(UserWorkspaceSelectionRepository userWorkspaceSelectionRepository,
                                                    AgentRepository agentRepository) {
        this.userWorkspaceSelectionRepository = userWorkspaceSelectionRepository;
        this.agentRepository = agentRepository;
    }

    public SidebarSelectionVO get() {
        Long userId = WorkspaceContext.require().userId();
        Long workspaceId = WorkspaceContext.require().workspaceId();
        return userWorkspaceSelectionRepository.findByUserAndWorkspace(userId, workspaceId)
                .map(selection -> new SidebarSelectionVO(selection.getSelectedAgentId()))
                .orElse(new SidebarSelectionVO(null));
    }

    @Transactional
    public SidebarSelectionVO update(UpdateSidebarSelectionRequest request) {
        Long userId = WorkspaceContext.require().userId();
        Long workspaceId = WorkspaceContext.require().workspaceId();
        Long selectedAgentId = request.selectedAgentId();
        if (selectedAgentId != null) {
            Agent agent = agentRepository.findById(selectedAgentId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_FOUND, "智能体不存在"));
            if (!workspaceId.equals(agent.getWorkspaceId())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "智能体不属于当前工作空间");
            }
        }
        UserWorkspaceSelection selection = userWorkspaceSelectionRepository
                .findByUserAndWorkspace(userId, workspaceId)
                .orElseGet(() -> {
                    UserWorkspaceSelection created = new UserWorkspaceSelection();
                    created.setUserId(userId);
                    created.setWorkspaceId(workspaceId);
                    return created;
                });
        selection.setSelectedAgentId(selectedAgentId);
        userWorkspaceSelectionRepository.saveOrUpdate(selection);
        return new SidebarSelectionVO(selectedAgentId);
    }
}
