package com.boxai.domain.platform;

import java.util.List;
import java.util.Optional;

public interface PlatformModelRepository {

    PlatformModel save(PlatformModel model);

    void update(PlatformModel model);

    void delete(Long id);

    Optional<PlatformModel> findById(Long id);

    List<PlatformModel> listActive();

    List<PlatformModel> listAll();
}
