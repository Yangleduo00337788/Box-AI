package com.boxai.domain.conversation;

import java.util.List;
import java.util.Optional;

public interface ChatProjectRepository {

    ChatProject save(ChatProject project);

    void update(ChatProject project);

    Optional<ChatProject> findById(Long id);

    List<ChatProject> listByWorkspaceAndUser(Long workspaceId, Long userId);

    int countConversations(Long projectId);

    void clearConversations(Long projectId);

    void delete(Long id);
}
