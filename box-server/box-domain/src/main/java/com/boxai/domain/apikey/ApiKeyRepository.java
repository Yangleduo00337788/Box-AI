package com.boxai.domain.apikey;

import java.util.List;
import java.util.Optional;

public interface ApiKeyRepository {

    ApiKey save(ApiKey apiKey);

    void update(ApiKey apiKey);

    Optional<ApiKey> findById(Long id);

    Optional<ApiKey> findByKeyHash(String keyHash);

    List<ApiKey> listByWorkspace(Long workspaceId);

    void delete(Long id);
}
