package com.boxai.domain.user;

import java.util.Optional;

public interface UserRepository {

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    User save(User user);

    void updateLastLogin(Long userId);

    void updateProfile(Long userId, String nickname, String bio, String avatarUrl);

    void updatePasswordHash(Long userId, String passwordHash);
}
