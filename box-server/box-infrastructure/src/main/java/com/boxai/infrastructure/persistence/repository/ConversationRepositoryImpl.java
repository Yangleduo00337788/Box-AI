package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.conversation.Conversation;
import com.boxai.domain.conversation.ConversationRepository;
import com.boxai.infrastructure.persistence.entity.ConversationDO;
import com.boxai.infrastructure.persistence.mapper.ConversationMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ConversationRepositoryImpl implements ConversationRepository {

    private final ConversationMapper mapper;

    public ConversationRepositoryImpl(ConversationMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Conversation save(Conversation conversation) {
        ConversationDO row = toDo(conversation);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        row.setDeleted(0);
        mapper.insert(row);
        conversation.setId(row.getId());
        conversation.setCreatedAt(row.getCreatedAt());
        conversation.setUpdatedAt(row.getUpdatedAt());
        return conversation;
    }

    @Override
    public void update(Conversation conversation) {
        ConversationDO row = toDo(conversation);
        row.setId(conversation.getId());
        row.setUpdatedAt(LocalDateTime.now());
        mapper.update(row);
        conversation.setUpdatedAt(row.getUpdatedAt());
    }

    @Override
    public Optional<Conversation> findById(Long id) {
        return Optional.ofNullable(mapper.selectOneById(id)).map(this::toDomain);
    }

    @Override
    public List<Conversation> listByWorkspaceAndUser(Long workspaceId, Long userId) {
        return mapper.selectListByQuery(
                        QueryWrapper.create()
                                .eq("workspace_id", workspaceId)
                                .eq("user_id", userId)
                                .orderBy("last_message_at", false)
                                .orderBy("updated_at", false))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Conversation> searchByTitle(Long workspaceId, Long userId, String keyword, int limit) {
        return mapper.selectListByQuery(
                        QueryWrapper.create()
                                .eq("workspace_id", workspaceId)
                                .eq("user_id", userId)
                                .like("title", keyword)
                                .orderBy("last_message_at", false)
                                .orderBy("updated_at", false)
                                .limit(limit))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public int countByWorkspaceAndUser(Long workspaceId, Long userId) {
        Long count = mapper.selectCountByQuery(
                QueryWrapper.create().eq("workspace_id", workspaceId).eq("user_id", userId));
        return count == null ? 0 : count.intValue();
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    private Conversation toDomain(ConversationDO row) {
        Conversation conversation = new Conversation();
        conversation.setId(row.getId());
        conversation.setWorkspaceId(row.getWorkspaceId());
        conversation.setAgentId(row.getAgentId());
        conversation.setAgentVersionId(row.getAgentVersionId());
        conversation.setUserId(row.getUserId());
        conversation.setTitle(row.getTitle());
        conversation.setStatus(row.getStatus());
        conversation.setMessageCount(row.getMessageCount());
        conversation.setLastMessageAt(row.getLastMessageAt());
        conversation.setMetadataJson(row.getMetadata());
        conversation.setCreatedAt(row.getCreatedAt());
        conversation.setUpdatedAt(row.getUpdatedAt());
        return conversation;
    }

    private ConversationDO toDo(Conversation conversation) {
        ConversationDO row = new ConversationDO();
        row.setWorkspaceId(conversation.getWorkspaceId());
        row.setAgentId(conversation.getAgentId());
        row.setAgentVersionId(conversation.getAgentVersionId());
        row.setUserId(conversation.getUserId());
        row.setTitle(conversation.getTitle());
        row.setStatus(conversation.getStatus() == null ? "ACTIVE" : conversation.getStatus());
        row.setMessageCount(conversation.getMessageCount() == null ? 0 : conversation.getMessageCount());
        row.setLastMessageAt(conversation.getLastMessageAt());
        row.setMetadata(conversation.getMetadataJson());
        return row;
    }
}
