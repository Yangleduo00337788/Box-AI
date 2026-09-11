package com.boxai.domain.user;

import java.util.Optional;

public interface UserPreferenceRepository {

    Optional<UserPreference> findByUserId(Long userId);

    UserPreference save(UserPreference preference);

    void update(UserPreference preference);
}
