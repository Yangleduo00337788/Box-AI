package com.boxai.user.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.session.UserSession;
import com.boxai.domain.session.UserSessionRepository;
import com.boxai.security.audit.HttpRequestContext;
import com.boxai.security.jwt.JwtProperties;
import com.boxai.user.api.UserSessionVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserSessionApplicationService {

    private final UserSessionRepository userSessionRepository;
    private final JwtProperties jwtProperties;

    public UserSessionApplicationService(UserSessionRepository userSessionRepository, JwtProperties jwtProperties) {
        this.userSessionRepository = userSessionRepository;
        this.jwtProperties = jwtProperties;
    }

    public String createSession(Long userId) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        UserSession session = new UserSession();
        session.setUserId(userId);
        session.setSessionId(sessionId);
        session.setDeviceName(parseDeviceName(HttpRequestContext.userAgent()));
        session.setIpAddress(HttpRequestContext.clientIp());
        session.setUserAgent(HttpRequestContext.userAgent());
        session.setLastActiveAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusSeconds(jwtProperties.getExpireSeconds()));
        session.setRevoked(0);
        userSessionRepository.save(session);
        return sessionId;
    }

    public List<UserSessionVO> listSessions(Long userId, String currentSessionId) {
        return userSessionRepository.listByUserId(userId).stream()
                .map(item -> new UserSessionVO(
                        item.getSessionId(),
                        item.getDeviceName(),
                        item.getIpAddress(),
                        item.getLastActiveAt(),
                        item.getExpiresAt(),
                        item.getSessionId().equals(currentSessionId)))
                .toList();
    }

    @Transactional
    public void revokeSession(Long userId, String sessionId) {
        userSessionRepository.findBySessionId(sessionId)
                .filter(item -> item.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "会话不存在"));
        userSessionRepository.revoke(userId, sessionId);
    }

    private String parseDeviceName(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "未知设备";
        }
        if (userAgent.contains("Mobile")) {
            return "移动设备";
        }
        if (userAgent.contains("Windows")) {
            return "Windows 浏览器";
        }
        if (userAgent.contains("Mac")) {
            return "Mac 浏览器";
        }
        return "浏览器";
    }
}
