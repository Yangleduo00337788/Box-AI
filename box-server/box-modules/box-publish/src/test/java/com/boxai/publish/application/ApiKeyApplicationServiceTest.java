package com.boxai.publish.application;

import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.apikey.ApiKey;
import com.boxai.domain.apikey.ApiKeyRepository;
import com.boxai.infrastructure.redis.RedisService;
import com.boxai.publish.api.CreateApiKeyRequest;
import com.boxai.publish.support.ApiKeyGenerator;
import com.boxai.publish.support.ApiKeyHasher;
import com.boxai.security.audit.AuditLogService;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.permission.WorkspacePermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyApplicationServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;
    @Mock
    private ApiKeyGenerator apiKeyGenerator;
    @Mock
    private ApiKeyHasher apiKeyHasher;
    @Mock
    private RedisService redisService;
    @Mock
    private WorkspacePermissionService workspacePermissionService;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ApiKeyApplicationService service;

    @AfterEach
    void tearDown() {
        WorkspaceContext.clear();
    }

    @Test
    void createReturnsRawKeyOnce() {
        WorkspaceContext.set(new WorkspaceContext(5L, 8L, 1L, "MEMBER"));
        when(apiKeyGenerator.generate()).thenReturn("box_live_secret");
        when(apiKeyGenerator.prefix("box_live_secret")).thenReturn("box_l");
        when(apiKeyHasher.hash("box_live_secret")).thenReturn("hash");

        CreateApiKeyRequest request = new CreateApiKeyRequest(" CI Key ");
        var response = service.create(request);

        verify(workspacePermissionService).requirePermission(PermissionCodes.API_KEY_MANAGE);
        verify(apiKeyRepository).save(any(ApiKey.class));
        verify(redisService).set(eq("box:api-key:hash"), any(), any());
        assertEquals("box_live_secret", response.apiKey());
        assertEquals("CI Key", response.name());
    }

    @Test
    void deleteRejectsForeignWorkspaceKey() {
        WorkspaceContext.set(new WorkspaceContext(5L, 8L, 1L, "MEMBER"));
        ApiKey apiKey = new ApiKey();
        apiKey.setId(2L);
        apiKey.setWorkspaceId(99L);
        apiKey.setKeyHash("hash");
        when(apiKeyRepository.findById(2L)).thenReturn(Optional.of(apiKey));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete(2L));
        assertEquals(ErrorCode.WORKSPACE_ACCESS_DENIED, ex.getCode());
        verify(workspacePermissionService).requirePermission(PermissionCodes.API_KEY_MANAGE);
    }
}
