package com.boxai.domain.config;

import java.util.List;
import java.util.Optional;

public interface SystemConfigRepository {

    Optional<SystemConfig> findByKey(String configKey);

    List<SystemConfig> findByKeys(List<String> configKeys);

    List<SystemConfig> listAll();

    void upsert(SystemConfig config);
}
