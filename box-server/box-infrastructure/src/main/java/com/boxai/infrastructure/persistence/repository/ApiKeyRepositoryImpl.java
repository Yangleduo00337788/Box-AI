package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.apikey.ApiKey;
import com.boxai.domain.apikey.ApiKeyRepository;
import com.boxai.infrastructure.persistence.entity.ApiKeyDO;
import com.boxai.infrastructure.persistence.mapper.ApiKeyMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ApiKeyRepositoryImpl implements ApiKeyRepository {

    private final ApiKeyMapper mapper;

    public ApiKeyRepositoryImpl(ApiKeyMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ApiKey save(ApiKey apiKey) {
        ApiKeyDO row = toDo(apiKey);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        row.setDeleted(0);
        mapper.insert(row);
        apiKey.setId(row.getId());
        apiKey.setCreatedAt(row.getCreatedAt());
        apiKey.setUpdatedAt(row.getUpdatedAt());
        return apiKey;
    }

    @Override
    public void update(ApiKey apiKey) {
        ApiKeyDO row = toDo(apiKey);
        row.setId(apiKey.getId());
        row.setUpdatedAt(LocalDateTime.now());
        mapper.update(row);
        apiKey.setUpdatedAt(row.getUpdatedAt());
    }

    @Override
    public Optional<ApiKey> findById(Long id) {
        return Optional.ofNullable(mapper.selectOneById(id)).map(this::toDomain);
    }

    @Override
    public Optional<ApiKey> findByKeyHash(String keyHash) {
        return Optional.ofNullable(
                        mapper.selectOneByQuery(QueryWrapper.create().eq("key_hash", keyHash)))
                .map(this::toDomain);
    }

    @Override
    public List<ApiKey> listByWorkspace(Long workspaceId) {
        return mapper.selectListByQuery(
                        QueryWrapper.create().eq("workspace_id", workspaceId).orderBy("created_at", false))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    private ApiKey toDomain(ApiKeyDO row) {
        ApiKey apiKey = new ApiKey();
        apiKey.setId(row.getId());
        apiKey.setWorkspaceId(row.getWorkspaceId());
        apiKey.setName(row.getName());
        apiKey.setKeyPrefix(row.getKeyPrefix());
        apiKey.setKeyHash(row.getKeyHash());
        apiKey.setStatus(row.getStatus());
        apiKey.setExpiresAt(row.getExpiresAt());
        apiKey.setLastUsedAt(row.getLastUsedAt());
        apiKey.setCreatedBy(row.getCreatedBy());
        apiKey.setCreatedAt(row.getCreatedAt());
        apiKey.setUpdatedAt(row.getUpdatedAt());
        return apiKey;
    }

    private ApiKeyDO toDo(ApiKey apiKey) {
        ApiKeyDO row = new ApiKeyDO();
        row.setWorkspaceId(apiKey.getWorkspaceId());
        row.setName(apiKey.getName());
        row.setKeyPrefix(apiKey.getKeyPrefix());
        row.setKeyHash(apiKey.getKeyHash());
        row.setStatus(apiKey.getStatus() == null ? 1 : apiKey.getStatus());
        row.setExpiresAt(apiKey.getExpiresAt());
        row.setLastUsedAt(apiKey.getLastUsedAt());
        row.setCreatedBy(apiKey.getCreatedBy());
        return row;
    }
}
