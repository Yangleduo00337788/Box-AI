package com.boxai.domain.platform;

import java.util.List;
import java.util.Optional;

public interface PlatformModelRepository {

    PlatformModel save(PlatformModel model);

    void update(PlatformModel model);

    void delete(Long id);

    Optional<PlatformModel> findById(Long id);

    Optional<PlatformModel> findByProviderAndCode(Long providerId, String modelCode);

    Optional<PlatformModel> findByProviderAndCodeIncludingDeleted(Long providerId, String modelCode);

    void restore(Long id);

    List<PlatformModel> listActive();

    List<PlatformModel> listAll();

    List<PlatformModel> listByProvider(Long providerId);

    void updateLimits(Long id, Integer contextWindow, Integer maxOutputTokens);

    void deleteByProvider(Long providerId);
}
