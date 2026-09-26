package com.boxai.user.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.session.UserSession;
import com.boxai.domain.session.UserSessionRepository;
import com.boxai.security.jwt.JwtProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSessionApplicationServiceTest {

    @Mock
    private UserSessionRepository userSessionRepository;

    @InjectMocks
    private UserSessionApplicationService service;

    private final JwtProperties jwtProperties = new JwtProperties();

    @org.junit.jupiter.api.BeforeEach
    void injectProperties() {
        service = new UserSessionApplicationService(userSessionRepository, jwtProperties);
    }

    @Test
    void createSessionPersistsUnknownDeviceWhenNoUserAgent() {
        String sessionId = service.createSession(5L);

        ArgumentCaptor<UserSession> captor = ArgumentCaptor.forClass(UserSession.class);
        verify(userSessionRepository).save(captor.capture());
        UserSession saved = captor.getValue();
        assertEquals(5L, saved.getUserId());
        assertEquals(sessionId, saved.getSessionId());
        assertEquals("未知设备", saved.getDeviceName());
        assertEquals(0, saved.getRevoked());
        assertTrue(saved.getExpiresAt().isAfter(LocalDateTime.now().plusHours(23)));
    }

    @Test
    void listSessionsMarksCurrentSession() {
        UserSession current = session("aaa", 5L);
        UserSession other = session("bbb", 5L);
        when(userSessionRepository.listByUserId(5L)).thenReturn(List.of(current, other));

        var list = service.listSessions(5L, "aaa");

        assertTrue(list.get(0).current());
        assertFalse(list.get(1).current());
    }

    @Test
    void revokeSessionRejectsForeignSession() {
        UserSession session = session("sid", 9L);
        when(userSessionRepository.findBySessionId("sid")).thenReturn(Optional.of(session));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.revokeSession(5L, "sid"));
        assertEquals(ErrorCode.NOT_FOUND, ex.getCode());
        verify(userSessionRepository, never()).revoke(5L, "sid");
    }

    @Test
    void revokeSessionRevokesOwnSession() {
        UserSession session = session("sid", 5L);
        when(userSessionRepository.findBySessionId("sid")).thenReturn(Optional.of(session));

        service.revokeSession(5L, "sid");

        verify(userSessionRepository).revoke(5L, "sid");
    }

    private static UserSession session(String id, Long userId) {
        UserSession session = new UserSession();
        session.setSessionId(id);
        session.setUserId(userId);
        session.setDeviceName("浏览器");
        return session;
    }
}
