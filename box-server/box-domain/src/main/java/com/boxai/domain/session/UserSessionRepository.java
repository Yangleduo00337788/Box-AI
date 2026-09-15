package com.boxai.domain.session;

import java.util.List;
import java.util.Optional;

public interface UserSessionRepository {

    UserSession save(UserSession session);

    void update(UserSession session);

    Optional<UserSession> findBySessionId(String sessionId);

    List<UserSession> listByUserId(Long userId);

    void revoke(Long userId, String sessionId);
}
