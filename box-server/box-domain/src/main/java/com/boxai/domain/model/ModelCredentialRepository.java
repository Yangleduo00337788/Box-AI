package com.boxai.domain.model;

import java.util.List;
import java.util.Optional;

public interface ModelCredentialRepository {

    ModelCredential save(ModelCredential credential);

    Optional<ModelCredential> findById(Long id);

    Optional<ModelCredential> findActiveByProvider(Long workspaceId, Long providerId);

    List<ModelCredential> listByWorkspace(Long workspaceId);

    void delete(Long id);

    void touchLastUsed(Long id);
}
