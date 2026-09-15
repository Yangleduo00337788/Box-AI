package com.boxai.conversation.application;

import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.conversation.api.ChatProjectVO;
import com.boxai.conversation.api.CreateChatProjectRequest;
import com.boxai.conversation.api.RenameChatProjectRequest;
import com.boxai.domain.conversation.ChatProject;
import com.boxai.domain.conversation.ChatProjectRepository;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.permission.WorkspacePermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ChatProjectApplicationService {

    private final ChatProjectRepository chatProjectRepository;
    private final WorkspacePermissionService workspacePermissionService;

    public ChatProjectApplicationService(ChatProjectRepository chatProjectRepository,
                                         WorkspacePermissionService workspacePermissionService) {
        this.chatProjectRepository = chatProjectRepository;
        this.workspacePermissionService = workspacePermissionService;
    }

    public List<ChatProjectVO> list() {
        workspacePermissionService.requirePermission(PermissionCodes.AGENT_READ);
        Long userId = WorkspaceContext.require().userId();
        return chatProjectRepository.listByWorkspaceAndUser(workspaceId(), userId).stream()
                .map(this::toVO)
                .toList();
    }

    @Transactional
    public ChatProjectVO create(CreateChatProjectRequest request) {
        workspacePermissionService.requirePermission(PermissionCodes.AGENT_READ);
        Long userId = WorkspaceContext.require().userId();
        ChatProject project = new ChatProject();
        project.setWorkspaceId(workspaceId());
        project.setUserId(userId);
        project.setName(request.name().trim());
        project.setSortOrder(0);
        chatProjectRepository.save(project);
        return toVO(project);
    }

    @Transactional
    public ChatProjectVO rename(Long id, RenameChatProjectRequest request) {
        workspacePermissionService.requirePermission(PermissionCodes.AGENT_READ);
        ChatProject project = requireProject(id);
        project.setName(request.name().trim());
        chatProjectRepository.update(project);
        return toVO(project);
    }

    @Transactional
    public void delete(Long id) {
        workspacePermissionService.requirePermission(PermissionCodes.AGENT_READ);
        requireProject(id);
        chatProjectRepository.clearConversations(id);
        chatProjectRepository.delete(id);
    }

    public ChatProject requireProject(Long id) {
        return findAccessible(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "项目不存在"));
    }

    public Optional<ChatProject> findAccessible(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        workspacePermissionService.requirePermission(PermissionCodes.AGENT_READ);
        return chatProjectRepository.findById(id).filter(this::ownedByCurrentUser);
    }

    private boolean ownedByCurrentUser(ChatProject project) {
        if (!workspaceId().equals(project.getWorkspaceId())) {
            return false;
        }
        Long userId = WorkspaceContext.require().userId();
        return userId.equals(project.getUserId());
    }

    private ChatProjectVO toVO(ChatProject project) {
        return new ChatProjectVO(
                project.getId(),
                project.getName(),
                chatProjectRepository.countConversations(project.getId()),
                project.getCreatedAt(),
                project.getUpdatedAt());
    }

    private Long workspaceId() {
        return WorkspaceContext.require().workspaceId();
    }
}
