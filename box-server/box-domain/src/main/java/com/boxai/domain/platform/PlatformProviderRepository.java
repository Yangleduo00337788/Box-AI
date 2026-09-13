package com.boxai.domain.platform;

import java.util.List;
import java.util.Optional;

public interface PlatformProviderRepository {

    PlatformProvider save(PlatformProvider provider);

    void update(PlatformProvider provider);

    void delete(Long id);

    Optional<PlatformProvider> findById(Long id);

    Optional<PlatformProvider> findByCode(String providerCode);

    List<PlatformProvider> listAll();
}
