package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.user.UserWorkspaceSelection;
import com.boxai.domain.user.UserWorkspaceSelectionRepository;
import com.boxai.infrastructure.persistence.entity.UserWorkspaceSelectionDO;
import com.boxai.infrastructure.persistence.mapper.UserWorkspaceSelectionMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class UserWorkspaceSelectionRepositoryImpl implements UserWorkspaceSelectionRepository {

    private final UserWorkspaceSelectionMapper mapper;

    public UserWorkspaceSelectionRepositoryImpl(UserWorkspaceSelectionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<UserWorkspaceSelection> findByUserAndWorkspace(Long userId, Long workspaceId) {
        return Optional.ofNullable(mapper.selectOneByQuery(QueryWrapper.create()
                        .eq("user_id", userId)
                        .eq("workspace_id", workspaceId)))
                .map(this::toDomain);
    }

    @Override
    public void save(UserWorkspaceSelection selection) {
        UserWorkspaceSelectionDO row = toDo(selection);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        mapper.insert(row);
        selection.setCreatedAt(row.getCreatedAt());
        selection.setUpdatedAt(row.getUpdatedAt());
    }

    @Override
    public void update(UserWorkspaceSelection selection) {
        UserWorkspaceSelectionDO row = toDo(selection);
        row.setUpdatedAt(LocalDateTime.now());
        mapper.update(row);
        selection.setUpdatedAt(row.getUpdatedAt());
    }

    @Override
    public void saveOrUpdate(UserWorkspaceSelection selection) {
        if (findByUserAndWorkspace(selection.getUserId(), selection.getWorkspaceId()).isPresent()) {
            update(selection);
            return;
        }
        try {
            save(selection);
        } catch (DuplicateKeyException ex) {
            update(selection);
        }
    }

    private UserWorkspaceSelection toDomain(UserWorkspaceSelectionDO row) {
        UserWorkspaceSelection selection = new UserWorkspaceSelection();
        selection.setUserId(row.getUserId());
        selection.setWorkspaceId(row.getWorkspaceId());
        selection.setSelectedAgentId(row.getSelectedAgentId());
        selection.setCreatedAt(row.getCreatedAt());
        selection.setUpdatedAt(row.getUpdatedAt());
        return selection;
    }

    private UserWorkspaceSelectionDO toDo(UserWorkspaceSelection selection) {
        UserWorkspaceSelectionDO row = new UserWorkspaceSelectionDO();
        row.setUserId(selection.getUserId());
        row.setWorkspaceId(selection.getWorkspaceId());
        row.setSelectedAgentId(selection.getSelectedAgentId());
        return row;
    }
}
