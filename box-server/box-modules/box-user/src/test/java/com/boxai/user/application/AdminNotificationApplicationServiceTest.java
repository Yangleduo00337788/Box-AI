package com.boxai.user.application;

import com.boxai.common.constant.UserTypes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.conversation.MessageFeedbackRepository;
import com.boxai.domain.ops.OpsPlacementRepository;
import com.boxai.domain.platform.PlatformAdminInboxDismissRepository;
import com.boxai.domain.platform.PlatformAdminInboxReadRepository;
import com.boxai.security.context.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminNotificationApplicationServiceTest {

    @Mock
    private MessageFeedbackRepository messageFeedbackRepository;
    @Mock
    private OpsPlacementRepository opsPlacementRepository;
    @Mock
    private PlatformAdminInboxDismissRepository inboxDismissRepository;
    @Mock
    private PlatformAdminInboxReadRepository inboxReadRepository;

    @InjectMocks
    private AdminNotificationApplicationService service;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void inboxIncludesPendingFeedbackForOps() {
        authenticate("OPS");
        when(inboxDismissRepository.findDismissedKeys(1L)).thenReturn(Set.of());
        when(inboxReadRepository.findReadMarkers(1L)).thenReturn(Map.of());
        when(messageFeedbackRepository.countByRatingAndStatus("BAD", "PENDING")).thenReturn(2L);
        when(opsPlacementRepository.listActive(isNull(), eq("B"), any())).thenReturn(List.of());

        var inbox = service.inbox();

        assertEquals(1, inbox.unreadCount());
        assertEquals("feedback:pending", inbox.items().get(0).key());
        assertEquals("TASK", inbox.items().get(0).type());
        assertFalse(inbox.items().get(0).read());
        assertTrue(inbox.items().get(0).body().contains("2 条"));
    }

    @Test
    void inboxHidesFeedbackTasksForFinance() {
        authenticate("FINANCE");
        when(inboxDismissRepository.findDismissedKeys(1L)).thenReturn(Set.of());
        when(inboxReadRepository.findReadMarkers(1L)).thenReturn(Map.of());
        when(opsPlacementRepository.listActive(isNull(), eq("B"), any())).thenReturn(List.of());

        var inbox = service.inbox();

        assertEquals(0, inbox.unreadCount());
        assertTrue(inbox.items().isEmpty());
        verify(messageFeedbackRepository, never()).countByRatingAndStatus(any(), any());
    }

    @Test
    void dismissRejectsPendingFeedbackTask() {
        authenticate("OPS");
        BusinessException ex = assertThrows(BusinessException.class, () -> service.dismiss("feedback:pending"));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        verify(inboxDismissRepository, never()).dismiss(org.mockito.ArgumentMatchers.anyLong(), any());
    }

    @Test
    void dismissStoresOpsAnnouncementKey() {
        authenticate("OPS");
        service.dismiss("ops:12");
        verify(inboxDismissRepository).dismiss(1L, "ops:12");
    }

    private void authenticate(String platformRole) {
        LoginUser user = new LoginUser(1L, "admin", UserTypes.PLATFORM_ADMIN, platformRole);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }
}
