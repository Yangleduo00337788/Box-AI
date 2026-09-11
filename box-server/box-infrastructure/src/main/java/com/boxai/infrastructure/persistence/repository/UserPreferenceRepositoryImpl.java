package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.user.UserPreference;
import com.boxai.domain.user.UserPreferenceRepository;
import com.boxai.infrastructure.persistence.entity.UserPreferenceDO;
import com.boxai.infrastructure.persistence.mapper.UserPreferenceMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class UserPreferenceRepositoryImpl implements UserPreferenceRepository {

    private final UserPreferenceMapper mapper;

    public UserPreferenceRepositoryImpl(UserPreferenceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<UserPreference> findByUserId(Long userId) {
        return Optional.ofNullable(mapper.selectOneById(userId)).map(this::toDomain);
    }

    @Override
    public UserPreference save(UserPreference preference) {
        UserPreferenceDO row = toDo(preference);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        mapper.insert(row);
        preference.setCreatedAt(row.getCreatedAt());
        preference.setUpdatedAt(row.getUpdatedAt());
        return preference;
    }

    @Override
    public void update(UserPreference preference) {
        UserPreferenceDO row = toDo(preference);
        row.setUpdatedAt(LocalDateTime.now());
        mapper.update(row);
        preference.setUpdatedAt(row.getUpdatedAt());
    }

    private UserPreference toDomain(UserPreferenceDO row) {
        UserPreference preference = new UserPreference();
        preference.setUserId(row.getUserId());
        preference.setTheme(row.getTheme());
        preference.setSendWithEnter(row.getSendWithEnter() == null || row.getSendWithEnter() == 1);
        preference.setCreatedAt(row.getCreatedAt());
        preference.setUpdatedAt(row.getUpdatedAt());
        return preference;
    }

    private UserPreferenceDO toDo(UserPreference preference) {
        UserPreferenceDO row = new UserPreferenceDO();
        row.setUserId(preference.getUserId());
        row.setTheme(preference.getTheme() == null ? "light" : preference.getTheme());
        row.setSendWithEnter(preference.getSendWithEnter() == null || preference.getSendWithEnter() ? 1 : 0);
        return row;
    }
}
