package com.boxai.user.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.storage.StorageBackendKind;
import com.boxai.infrastructure.minio.MinioProperties;
import com.boxai.infrastructure.storage.ObjectStorageRuntime;
import com.boxai.infrastructure.storage.ObjectStorageSettingsDocument;
import com.boxai.user.api.platform.TestPlatformObjectStorageRequest;
import com.boxai.user.api.platform.UpdatePlatformObjectStorageBackendRequest;
import com.boxai.user.api.platform.UpdatePlatformObjectStorageRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformObjectStorageSettingsApplicationServiceTest {

    @Mock
    private ObjectStorageRuntime objectStorageRuntime;
    @Mock
    private MinioProperties yamlMinio;

    @InjectMocks
    private PlatformObjectStorageSettingsApplicationService service;

    @Test
    void getSettingsUsesYamlFallbackForMinio() {
        ObjectStorageSettingsDocument doc = new ObjectStorageSettingsDocument();
        doc.setActive("MINIO");
        doc.getMinio().setUseYamlFallback(true);
        when(objectStorageRuntime.snapshotDocument()).thenReturn(doc);
        when(objectStorageRuntime.activeBucket()).thenReturn("yaml-bucket");
        when(yamlMinio.getEndpoint()).thenReturn("http://127.0.0.1:9000");
        when(yamlMinio.getAccessKey()).thenReturn("box");
        when(yamlMinio.getSecretKey()).thenReturn("secret");
        when(yamlMinio.getBucket()).thenReturn("yaml-bucket");
        when(objectStorageRuntime.ping(StorageBackendKind.MINIO)).thenReturn(true);

        var vo = service.getSettings();

        assertEquals("MINIO", vo.activeBackend());
        assertEquals("yaml-bucket", vo.effectiveBucket());
        assertTrue(vo.minio().useYamlFallback());
        assertTrue(vo.minio().configured());
        assertTrue(vo.minio().reachable());
        assertFalse(vo.r2().configured());
    }

    @Test
    void updateRejectsWhenActiveBackendPingFails() {
        ObjectStorageSettingsDocument doc = new ObjectStorageSettingsDocument();
        when(objectStorageRuntime.snapshotDocument()).thenReturn(doc);
        when(objectStorageRuntime.pingDetail(StorageBackendKind.R2))
                .thenReturn(new ObjectStorageRuntime.PingResult(false, "Access Denied"));

        UpdatePlatformObjectStorageRequest request = new UpdatePlatformObjectStorageRequest(
                "R2",
                null,
                new UpdatePlatformObjectStorageBackendRequest(
                        "https://acct.r2.cloudflarestorage.com",
                        "ak",
                        "sk",
                        "box-ai",
                        null));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(request));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        verify(objectStorageRuntime).persistDocument(any());
    }

    @Test
    void updateRejectsNullRequest() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(null));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        verify(objectStorageRuntime, never()).persistDocument(any());
    }

    @Test
    void testConnectionDelegatesToRuntime() {
        when(objectStorageRuntime.ping(StorageBackendKind.R2)).thenReturn(true);
        assertTrue(service.testConnection(new TestPlatformObjectStorageRequest("R2")));
    }
}
