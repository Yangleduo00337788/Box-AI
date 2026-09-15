package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.conversation.ChatProject;
import com.boxai.domain.conversation.ChatProjectRepository;
import com.boxai.infrastructure.persistence.entity.ChatProjectDO;
import com.boxai.infrastructure.persistence.entity.ConversationDO;
import com.boxai.infrastructure.persistence.mapper.ChatProjectMapper;
import com.boxai.infrastructure.persistence.mapper.ConversationMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ChatProjectRepositoryImpl implements ChatProjectRepository {

    private final ChatProjectMapper mapper;
    private final ConversationMapper conversationMapper;

    public ChatProjectRepositoryImpl(ChatProjectMapper mapper, ConversationMapper conversationMapper) {
        this.mapper = mapper;
        this.conversationMapper = conversationMapper;
    }

    @Override
    public ChatProject save(ChatProject project) {
        ChatProjectDO row = toDo(project);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        row.setDeleted(0);
        mapper.insert(row);
        project.setId(row.getId());
        project.setCreatedAt(row.getCreatedAt());
        project.setUpdatedAt(row.getUpdatedAt());
        return project;
    }

    @Override
    public void update(ChatProject project) {
        ChatProjectDO row = toDo(project);
        row.setId(project.getId());
        row.setUpdatedAt(LocalDateTime.now());
        mapper.update(row);
        project.setUpdatedAt(row.getUpdatedAt());
    }

    @Override
    public Optional<ChatProject> findById(Long id) {
        return Optional.ofNullable(mapper.selectOneById(id)).map(this::toDomain);
    }

    @Override
    public List<ChatProject> listByWorkspaceAndUser(Long workspaceId, Long userId) {
        return mapper.selectListByQuery(
                        QueryWrapper.create()
                                .eq("workspace_id", workspaceId)
                                .eq("user_id", userId)
                                .orderBy("sort_order", true)
                                .orderBy("updated_at", false))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public int countConversations(Long projectId) {
        Long count = conversationMapper.selectCountByQuery(
                QueryWrapper.create().eq("project_id", projectId));
        return count == null ? 0 : count.intValue();
    }

    @Override
    public void clearConversations(Long projectId) {
        List<ConversationDO> rows = conversationMapper.selectListByQuery(
                QueryWrapper.create().eq("project_id", projectId));
        for (ConversationDO row : rows) {
            row.setProjectId(null);
            row.setUpdatedAt(LocalDateTime.now());
            conversationMapper.update(row);
        }
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    private ChatProject toDomain(ChatProjectDO row) {
        ChatProject project = new ChatProject();
        project.setId(row.getId());
        project.setWorkspaceId(row.getWorkspaceId());
        project.setUserId(row.getUserId());
        project.setName(row.getName());
        project.setSortOrder(row.getSortOrder());
        project.setCreatedAt(row.getCreatedAt());
        project.setUpdatedAt(row.getUpdatedAt());
        return project;
    }

    private ChatProjectDO toDo(ChatProject project) {
        ChatProjectDO row = new ChatProjectDO();
        row.setWorkspaceId(project.getWorkspaceId());
        row.setUserId(project.getUserId());
        row.setName(project.getName());
        row.setSortOrder(project.getSortOrder() == null ? 0 : project.getSortOrder());
        return row;
    }
}
