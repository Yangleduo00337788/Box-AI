package com.boxai.domain.user;

import com.boxai.common.result.PageResult;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository {

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    List<User> findByIds(Collection<Long> ids);

    PageResult<User> page(UserQuery query);

    long countByUserTypeAndStatus(String userType, Integer status);

    User save(User user);

    void updateLastLogin(Long userId);

    void updateStatus(Long userId, Integer status);

    void updateProfile(Long userId, String nickname, String bio, String avatarUrl);

    void updatePasswordHash(Long userId, String passwordHash);
}
