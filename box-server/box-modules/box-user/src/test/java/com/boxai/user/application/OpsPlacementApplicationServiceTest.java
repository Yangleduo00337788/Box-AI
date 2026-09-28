package com.boxai.user.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.ops.OpsPlacement;
import com.boxai.domain.ops.OpsPlacementRepository;
import com.boxai.user.api.CreateOpsPlacementRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpsPlacementApplicationServiceTest {

    @Mock
    private OpsPlacementRepository opsPlacementRepository;
    @Mock
    private OpsMetricsApplicationService opsMetricsApplicationService;

    @InjectMocks
    private OpsPlacementApplicationService service;

    @Test
    void createRejectsConsumerSlotOnAdminAudience() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(request(
                "B", "CHAT_HOME", "ANNOUNCEMENT", "/pricing", null, null, null)));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        verify(opsPlacementRepository, never()).save(any());
    }

    @Test
    void createRejectsUnsafeLink() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(request(
                "C", "CHAT_HOME", "ANNOUNCEMENT", "javascript:alert(1)", null, null, null)));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    }

    @Test
    void createRequiresBannerImage() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(request(
                "C", "CHAT_BANNER", "BANNER", "/pricing", null, null, null)));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    }

    @Test
    void createRejectsInvertedWindow() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(request(
                "C", "CHAT_HOME", "ANNOUNCEMENT", "/pricing",
                LocalDateTime.of(2026, 2, 1, 0, 0),
                LocalDateTime.of(2026, 1, 1, 0, 0),
                null)));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    }

    @Test
    void syncCrossAudienceCreatesConsumerInboxFromAdminHeader() {
        OpsPlacement source = new OpsPlacement();
        source.setId(10L);
        source.setAudience("B");
        source.setSlot("ADMIN_HEADER");
        source.setKind("ANNOUNCEMENT");
        source.setTitle("v1.0.0");
        source.setBody("release");
        source.setTheme("info");
        source.setDismissible(1);
        source.setStatus("LISTED");
        source.setSortOrder(0);

        when(opsPlacementRepository.findById(10L)).thenReturn(java.util.Optional.of(source));
        org.mockito.Mockito.doAnswer(invocation -> {
            OpsPlacement peer = invocation.getArgument(0);
            peer.setId(20L);
            return null;
        }).when(opsPlacementRepository).save(any(OpsPlacement.class));

        var vo = service.syncCrossAudience(10L);

        ArgumentCaptor<OpsPlacement> saveCaptor = ArgumentCaptor.forClass(OpsPlacement.class);
        verify(opsPlacementRepository).save(saveCaptor.capture());
        OpsPlacement peer = saveCaptor.getValue();
        assertEquals("C", peer.getAudience());
        assertEquals("CONSUMER_INBOX", peer.getSlot());
        assertEquals("v1.0.0", peer.getTitle());
        assertEquals(20L, vo.syncPeerId());
    }

    @Test
    void createPersistsChatHomeAnnouncement() {
        org.mockito.Mockito.doAnswer(invocation -> {
            OpsPlacement placement = invocation.getArgument(0);
            placement.setId(15L);
            return null;
        }).when(opsPlacementRepository).save(any(OpsPlacement.class));

        var vo = service.create(request(
                "C", "chat_home", "announcement", "/pricing", null, null, "star"));

        ArgumentCaptor<OpsPlacement> captor = ArgumentCaptor.forClass(OpsPlacement.class);
        verify(opsPlacementRepository).save(captor.capture());
        OpsPlacement saved = captor.getValue();
        assertEquals("C", saved.getAudience());
        assertEquals("CHAT_HOME", saved.getSlot());
        assertEquals("ANNOUNCEMENT", saved.getKind());
        assertEquals("/pricing", saved.getLinkUrl());
        assertEquals("star", saved.getIconName());
        assertEquals("LISTED", saved.getStatus());
        assertEquals("info", saved.getTheme());
        assertEquals(1, saved.getDismissible());
        assertEquals("公告", vo.title());
    }

    private static CreateOpsPlacementRequest request(String audience,
                                                     String slot,
                                                     String kind,
                                                     String link,
                                                     LocalDateTime startsAt,
                                                     LocalDateTime endsAt,
                                                     String iconName) {
        return new CreateOpsPlacementRequest(
                audience, slot, kind, " 公告 ", "body", link, "去看看",
                iconName, null, null, null, null, null, 1, startsAt, endsAt);
    }
}
