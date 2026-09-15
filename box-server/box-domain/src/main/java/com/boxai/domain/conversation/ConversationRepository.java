package com.boxai.domain.conversation;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository {

    Conversation save(Conversation conversation);

    void update(Conversation conversation);

    Optional<Conversation> findById(Long id);

    List<Conversation> listByWorkspaceAndUser(Long workspaceId, Long userId);

    List<Conversation> listByWorkspaceAndUser(Long workspaceId, Long userId, Long projectId, boolean unassignedOnly);

    List<Conversation> searchByTitle(Long workspaceId, Long userId, String keyword, int limit);

    int countByWorkspaceAndUser(Long workspaceId, Long userId);

    void delete(Long id);
}
