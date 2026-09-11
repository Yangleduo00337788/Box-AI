package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.user.UserSidebarPin;
import com.boxai.domain.user.UserSidebarPinRepository;
import com.boxai.infrastructure.persistence.entity.UserSidebarPinDO;
import com.boxai.infrastructure.persistence.mapper.UserSidebarPinMapper;
import com.mybatisflex.core.logicdelete.LogicDeleteManager;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class UserSidebarPinRepositoryImpl implements UserSidebarPinRepository {

    private final UserSidebarPinMapper mapper;

    public UserSidebarPinRepositoryImpl(UserSidebarPinMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<UserSidebarPin> listByUserAndWorkspace(Long userId, Long workspaceId) {
        return mapper.selectListByQuery(
                        QueryWrapper.create()
                                .eq("user_id", userId)
                                .eq("workspace_id", workspaceId)
                                .orderBy("sort_order", true)
                                .orderBy("id", true))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void replaceAll(Long userId, Long workspaceId, List<UserSidebarPin> pins) {
        LogicDeleteManager.execWithoutLogicDelete(() -> mapper.deleteByQuery(
                QueryWrapper.create().eq("user_id", userId).eq("workspace_id", workspaceId)));
        LocalDateTime now = LocalDateTime.now();
        for (UserSidebarPin pin : pins) {
            UserSidebarPinDO row = new UserSidebarPinDO();
            row.setUserId(userId);
            row.setWorkspaceId(workspaceId);
            row.setPinType(pin.getPinType());
            row.setTargetId(pin.getTargetId());
            row.setSortOrder(pin.getSortOrder() == null ? 0 : pin.getSortOrder());
            row.setCreatedAt(now);
            row.setUpdatedAt(now);
            row.setDeleted(0);
            mapper.insert(row);
        }
    }

    private UserSidebarPin toDomain(UserSidebarPinDO row) {
        UserSidebarPin pin = new UserSidebarPin();
        pin.setId(row.getId());
        pin.setUserId(row.getUserId());
        pin.setWorkspaceId(row.getWorkspaceId());
        pin.setPinType(row.getPinType());
        pin.setTargetId(row.getTargetId());
        pin.setSortOrder(row.getSortOrder());
        pin.setCreatedAt(row.getCreatedAt());
        pin.setUpdatedAt(row.getUpdatedAt());
        return pin;
    }
}
