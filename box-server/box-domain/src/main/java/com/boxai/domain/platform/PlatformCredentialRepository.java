package com.boxai.domain.platform;

import java.util.List;
import java.util.Optional;

public interface PlatformCredentialRepository {

    PlatformCredential save(PlatformCredential credential);

    void update(PlatformCredential credential);

    void delete(Long id);

    Optional<PlatformCredential> findById(Long id);

    Optional<PlatformCredential> findActiveByProvider(Long providerId);

    java.util.Set<Long> listActiveProviderIds();

    List<PlatformCredential> listByProvider(Long providerId);

    void touchLastUsed(Long id);
}
