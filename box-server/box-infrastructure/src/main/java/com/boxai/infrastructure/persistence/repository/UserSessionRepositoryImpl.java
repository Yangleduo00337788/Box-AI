package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.session.UserSession;
import com.boxai.domain.session.UserSessionRepository;
import com.boxai.infrastructure.persistence.entity.UserSessionDO;
import com.boxai.infrastructure.persistence.mapper.UserSessionMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class UserSessionRepositoryImpl implements UserSessionRepository {

    private final UserSessionMapper userSessionMapper;

    public UserSessionRepositoryImpl(UserSessionMapper userSessionMapper) {
        this.userSessionMapper = userSessionMapper;
    }

    @Override
    public UserSession save(UserSession session) {
        UserSessionDO row = toDo(session);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        userSessionMapper.insert(row);
        session.setId(row.getId());
        session.setCreatedAt(row.getCreatedAt());
        session.setUpdatedAt(row.getUpdatedAt());
        return session;
    }

    @Override
    public void update(UserSession session) {
        UserSessionDO row = toDo(session);
        row.setId(session.getId());
        row.setUpdatedAt(LocalDateTime.now());
        userSessionMapper.update(row);
        session.setUpdatedAt(row.getUpdatedAt());
    }

    @Override
    public Optional<UserSession> findBySessionId(String sessionId) {
        return Optional.ofNullable(userSessionMapper.selectOneByQuery(
                        QueryWrapper.create().eq("session_id", sessionId)))
                .map(this::toDomain);
    }

    @Override
    public List<UserSession> listByUserId(Long userId) {
        return userSessionMapper.selectListByQuery(
                        QueryWrapper.create()
                                .eq("user_id", userId)
                                .eq("revoked", 0)
                                .orderBy("last_active_at", false))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void revoke(Long userId, String sessionId) {
        userSessionMapper.selectListByQuery(
                        QueryWrapper.create().eq("user_id", userId).eq("session_id", sessionId))
                .forEach(row -> {
                    row.setRevoked(1);
                    row.setUpdatedAt(LocalDateTime.now());
                    userSessionMapper.update(row);
                });
    }

    private UserSession toDomain(UserSessionDO row) {
        UserSession session = new UserSession();
        session.setId(row.getId());
        session.setUserId(row.getUserId());
        session.setSessionId(row.getSessionId());
        session.setDeviceName(row.getDeviceName());
        session.setIpAddress(row.getIpAddress());
        session.setUserAgent(row.getUserAgent());
        session.setLastActiveAt(row.getLastActiveAt());
        session.setExpiresAt(row.getExpiresAt());
        session.setRevoked(row.getRevoked());
        session.setCreatedAt(row.getCreatedAt());
        session.setUpdatedAt(row.getUpdatedAt());
        return session;
    }

    private UserSessionDO toDo(UserSession session) {
        UserSessionDO row = new UserSessionDO();
        row.setUserId(session.getUserId());
        row.setSessionId(session.getSessionId());
        row.setDeviceName(session.getDeviceName());
        row.setIpAddress(session.getIpAddress());
        row.setUserAgent(session.getUserAgent());
        row.setLastActiveAt(session.getLastActiveAt());
        row.setExpiresAt(session.getExpiresAt());
        row.setRevoked(session.getRevoked() == null ? 0 : session.getRevoked());
        return row;
    }
}
