package com.boxai.infrastructure.persistence.repository;

import com.boxai.common.constant.UserTypes;
import com.boxai.domain.user.User;
import com.boxai.domain.user.UserRepository;
import com.boxai.infrastructure.persistence.entity.UserDO;
import com.boxai.infrastructure.persistence.mapper.UserMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        row.setDeleted(0);
        userMapper.insert(row);
        user.setId(row.getId());
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
        user.setLastLoginAt(row.getLastLoginAt());
        return user;
    }
}
