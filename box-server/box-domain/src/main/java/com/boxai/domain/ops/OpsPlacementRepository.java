package com.boxai.domain.ops;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OpsPlacementRepository {

    Optional<OpsPlacement> findById(Long id);

    List<OpsPlacement> listAllForAdmin();

    List<OpsPlacement> listActive(String slot, LocalDateTime now);

    OpsPlacement save(OpsPlacement placement);

    void update(OpsPlacement placement);

    void delete(Long id);
}
