package com.boxai.infrastructure.persistence.repository;

import com.boxai.common.constant.UserTypes;
import com.boxai.common.result.PageResult;
import com.boxai.domain.user.User;
import com.boxai.domain.user.UserQuery;
import com.boxai.domain.user.UserRepository;
import com.boxai.infrastructure.persistence.entity.UserDO;
import com.boxai.infrastructure.persistence.mapper.UserMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;

    public UserRepositoryImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(userMapper.selectOneById(id)).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        UserDO row = userMapper.selectOneByQuery(QueryWrapper.create().eq("email", email));
        return Optional.ofNullable(row).map(this::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        UserDO row = userMapper.selectOneByQuery(QueryWrapper.create().eq("username", username));
        return Optional.ofNullable(row).map(this::toDomain);
    }

    @Override
    public List<User> findByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return userMapper.selectListByQuery(QueryWrapper.create().in("id", ids)).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public PageResult<User> page(UserQuery query) {
        int page = Math.max(query.getPage(), 1);
        int pageSize = Math.min(Math.max(query.getPageSize(), 1), 100);
        QueryWrapper wrapper = QueryWrapper.create()
                .eq("user_type", query.getUserType(), query.getUserType() != null && !query.getUserType().isBlank())
                .eq("status", query.getStatus(), query.getStatus() != null)
                .orderBy("created_at", false);
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            String like = "%" + query.getKeyword().trim() + "%";
            wrapper.and("(email like {0} or nickname like {0} or username like {0})", like);
        }
        Page<UserDO> result = userMapper.paginate(page, pageSize, wrapper);
        List<User> records = result.getRecords().stream().map(this::toDomain).toList();
        return new PageResult<>(records, result.getTotalRow(), page, pageSize);
    }

    @Override
    public long countByUserTypeAndStatus(String userType, Integer status) {
        Long count = userMapper.selectCountByQuery(
                QueryWrapper.create().eq("user_type", userType).eq("status", status));
        return count == null ? 0 : count;
    }

    @Override
    public User save(User user) {
        UserDO row = new UserDO();
        row.setUsername(user.getUsername());
        row.setEmail(user.getEmail());
        row.setPhone(user.getPhone());
        row.setPasswordHash(user.getPasswordHash());
        row.setNickname(user.getNickname());
        row.setAvatarUrl(user.getAvatarUrl());
        row.setBio(user.getBio());
        row.setStatus(user.getStatus() == null ? 1 : user.getStatus());
        row.setUserType(user.getUserType() == null ? UserTypes.TENANT_USER : user.getUserType());
        row.setPlatformAdminRole(user.getPlatformAdminRole());
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        row.setDeleted(0);
        userMapper.insert(row);
        user.setId(row.getId());
        user.setCreatedAt(row.getCreatedAt());
        return user;
    }

    @Override
    public void updateLastLogin(Long userId) {
        UserDO patch = new UserDO();
        patch.setId(userId);
        patch.setLastLoginAt(LocalDateTime.now());
        patch.setUpdatedAt(LocalDateTime.now());
        userMapper.update(patch);
    }

    @Override
    public void updateStatus(Long userId, Integer status) {
        UserDO patch = new UserDO();
        patch.setId(userId);
        patch.setStatus(status);
        patch.setUpdatedAt(LocalDateTime.now());
        userMapper.update(patch);
    }

    @Override
    public void updateProfile(Long userId, String nickname, String bio, String avatarUrl) {
        UserDO patch = new UserDO();
        patch.setId(userId);
        if (nickname != null) {
            patch.setNickname(nickname);
        }
        if (bio != null) {
            patch.setBio(bio);
        }
        if (avatarUrl != null) {
            patch.setAvatarUrl(avatarUrl);
        }
        patch.setUpdatedAt(LocalDateTime.now());
        userMapper.update(patch);
    }

    @Override
    public void updatePasswordHash(Long userId, String passwordHash) {
        UserDO patch = new UserDO();
        patch.setId(userId);
        patch.setPasswordHash(passwordHash);
        patch.setUpdatedAt(LocalDateTime.now());
        userMapper.update(patch);
    }

    private User toDomain(UserDO row) {
        User user = new User();
        user.setId(row.getId());
        user.setUsername(row.getUsername());
        user.setEmail(row.getEmail());
        user.setPhone(row.getPhone());
        user.setPasswordHash(row.getPasswordHash());
        user.setNickname(row.getNickname());
        user.setAvatarUrl(row.getAvatarUrl());
        user.setBio(row.getBio());
        user.setStatus(row.getStatus());
        user.setUserType(row.getUserType());
        user.setPlatformAdminRole(row.getPlatformAdminRole());
        user.setLastLoginAt(row.getLastLoginAt());
        user.setCreatedAt(row.getCreatedAt());
        return user;
    }
}
