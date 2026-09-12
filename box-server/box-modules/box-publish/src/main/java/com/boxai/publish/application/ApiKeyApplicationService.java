package com.boxai.publish.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.apikey.ApiKey;
import com.boxai.domain.apikey.ApiKeyRepository;
import com.boxai.infrastructure.redis.RedisService;
import com.boxai.publish.api.ApiKeyVO;
import com.boxai.publish.api.CreateApiKeyRequest;
import com.boxai.publish.api.CreateApiKeyResponse;
import com.boxai.publish.support.ApiKeyGenerator;
import com.boxai.publish.support.ApiKeyHasher;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.permission.WorkspacePermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
public class ApiKeyApplicationService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyGenerator apiKeyGenerator;
    private final ApiKeyHasher apiKeyHasher;
    private final RedisService redisService;
    private final WorkspacePermissionService workspacePermissionService;

    public ApiKeyApplicationService(ApiKeyRepository apiKeyRepository,
                                    ApiKeyGenerator apiKeyGenerator,
                                    ApiKeyHasher apiKeyHasher,
                                    RedisService redisService,
                                    WorkspacePermissionService workspacePermissionService) {
        this.apiKeyRepository = apiKeyRepository;
        this.apiKeyGenerator = apiKeyGenerator;
        this.apiKeyHasher = apiKeyHasher;
        this.redisService = redisService;
        this.workspacePermissionService = workspacePermissionService;
    }

    public List<ApiKeyVO> list() {
        workspacePermissionService.requirePermission("agent:publish");
        return apiKeyRepository.listByWorkspace(workspaceId()).stream().map(this::toVO).toList();
    }

    @Transactional
    public CreateApiKeyResponse create(CreateApiKeyRequest request) {
        workspacePermissionService.requirePermission("agent:publish");
        String rawKey = apiKeyGenerator.generate();
        ApiKey apiKey = new ApiKey();
        apiKey.setWorkspaceId(workspaceId());
        apiKey.setName(request.name().trim());
        apiKey.setKeyPrefix(apiKeyGenerator.prefix(rawKey));
        apiKey.setKeyHash(apiKeyHasher.hash(rawKey));
        apiKey.setStatus(1);
        apiKey.setCreatedBy(WorkspaceContext.require().userId());
        apiKeyRepository.save(apiKey);
        cache(apiKey);
        return new CreateApiKeyResponse(apiKey.getId(), apiKey.getName(), rawKey);
    }

    @Transactional
    public void delete(Long id) {
        workspacePermissionService.requirePermission("agent:publish");
        ApiKey apiKey = requireApiKey(id);
        apiKeyRepository.delete(apiKey.getId());
        evictCache(apiKey.getKeyHash());
    }

    @Transactional
    public ApiKeyVO disable(Long id) {
        workspacePermissionService.requirePermission("agent:publish");
        ApiKey apiKey = requireApiKey(id);
        apiKey.setStatus(0);
        apiKeyRepository.update(apiKey);
        evictCache(apiKey.getKeyHash());
        return toVO(apiKey);
    }

    @Transactional
    public ApiKeyVO enable(Long id) {
        workspacePermissionService.requirePermission("agent:publish");
        ApiKey apiKey = requireApiKey(id);
        apiKey.setStatus(1);
        apiKeyRepository.update(apiKey);
        cache(apiKey);
        return toVO(apiKey);
    }

    @Transactional
    public CreateApiKeyResponse rotate(Long id) {
        ApiKey apiKey = requireApiKey(id);
        evictCache(apiKey.getKeyHash());
        String rawKey = apiKeyGenerator.generate();
        apiKey.setKeyPrefix(apiKeyGenerator.prefix(rawKey));
        apiKey.setKeyHash(apiKeyHasher.hash(rawKey));
        apiKey.setStatus(1);
        apiKeyRepository.update(apiKey);
        cache(apiKey);
        return new CreateApiKeyResponse(apiKey.getId(), apiKey.getName(), rawKey);
    }

    public ApiKey authenticate(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.isBlank()) {
            throw new BusinessException(ErrorCode.API_KEY_INVALID, "API Key 无效");
        }
        String hash = apiKeyHasher.hash(rawApiKey);
        String cached = redisService.get(cacheKey(hash));
        if (cached != null) {
            Long id = Long.valueOf(cached);
            ApiKey apiKey = apiKeyRepository.findById(id)
                    .orElseThrow(() -> new BusinessException(ErrorCode.API_KEY_INVALID, "API Key 无效"));
            assertUsable(apiKey);
            return apiKey;
        }
        ApiKey apiKey = apiKeyRepository.findByKeyHash(hash)
                .orElseThrow(() -> new BusinessException(ErrorCode.API_KEY_INVALID, "API Key 无效"));
        assertUsable(apiKey);
        cache(apiKey);
        return apiKey;
    }

    @Transactional
    public void touchLastUsed(ApiKey apiKey) {
        apiKey.setLastUsedAt(java.time.LocalDateTime.now());
        apiKeyRepository.update(apiKey);
    }

    private void assertUsable(ApiKey apiKey) {
        if (apiKey.getStatus() == null || apiKey.getStatus() != 1) {
            throw new BusinessException(ErrorCode.API_KEY_DISABLED, "API Key 已禁用");
        }
        if (apiKey.getExpiresAt() != null && apiKey.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.API_KEY_INVALID, "API Key 已过期");
        }
    }

    private ApiKey requireApiKey(Long id) {
        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.API_KEY_NOT_FOUND, "API Key 不存在"));
        if (!workspaceId().equals(apiKey.getWorkspaceId())) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "无权访问该 API Key");
        }
        return apiKey;
    }

    private ApiKeyVO toVO(ApiKey apiKey) {
        return new ApiKeyVO(
                apiKey.getId(),
                apiKey.getName(),
                apiKey.getKeyPrefix(),
                apiKey.getStatus(),
                apiKey.getExpiresAt(),
                apiKey.getLastUsedAt(),
                apiKey.getCreatedAt());
    }

    private void cache(ApiKey apiKey) {
        redisService.set(cacheKey(apiKey.getKeyHash()), String.valueOf(apiKey.getId()), CACHE_TTL);
    }

    private void evictCache(String keyHash) {
        redisService.delete(cacheKey(keyHash));
    }

    private String cacheKey(String keyHash) {
        return "box:api-key:" + keyHash;
    }

    private Long workspaceId() {
        return WorkspaceContext.require().workspaceId();
    }
}
