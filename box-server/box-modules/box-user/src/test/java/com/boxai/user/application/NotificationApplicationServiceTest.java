package com.boxai.user.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.notification.Notification;
import com.boxai.domain.notification.NotificationRepository;
import com.boxai.security.context.WorkspaceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationApplicationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationApplicationService service;

    @AfterEach
    void tearDown() {
        WorkspaceContext.clear();
    }

    @Test
    void listClampsLimitBetweenOneAndOneHundred() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        when(notificationRepository.listByUser(3L, 7L, 20)).thenReturn(java.util.List.of());
        service.list(0);
        verify(notificationRepository).listByUser(3L, 7L, 20);

        when(notificationRepository.listByUser(3L, 7L, 100)).thenReturn(java.util.List.of());
        service.list(500);
        verify(notificationRepository).listByUser(3L, 7L, 100);
    }

    @Test
    void markReadRejectsMissingNotification() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        when(notificationRepository.findById(11L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.markRead(11L));
        assertEquals(ErrorCode.NOT_FOUND, ex.getCode());
    }

    @Test
    void createDefaultsCategoryToSystem() {
        var vo = service.create(3L, 7L, "Title", "Body", null, "/inbox");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals("SYSTEM", captor.getValue().getCategory());
        assertFalse(captor.getValue().getRead());
        assertEquals("Title", vo.title());
        assertEquals("SYSTEM", vo.category());
    }
}
