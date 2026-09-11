package com.boxai.domain.publish;

import java.util.Optional;

public interface PublishRepository {

    Publish save(Publish publish);

    void update(Publish publish);

    Optional<Publish> findLatestActive(String resourceType, Long resourceId);

    void revokeByResource(String resourceType, Long resourceId);
}
