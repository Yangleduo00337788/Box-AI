package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.publish.Publish;
import com.boxai.domain.publish.PublishRepository;
import com.boxai.infrastructure.persistence.entity.PublishDO;
import com.boxai.infrastructure.persistence.mapper.PublishMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class PublishRepositoryImpl implements PublishRepository {

    private final PublishMapper mapper;

    public PublishRepositoryImpl(PublishMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Publish save(Publish publish) {
        PublishDO row = toDo(publish);
        row.setCreatedAt(LocalDateTime.now());
        mapper.insert(row);
        publish.setId(row.getId());
        publish.setCreatedAt(row.getCreatedAt());
        return publish;
    }

    @Override
    public void update(Publish publish) {
        PublishDO row = toDo(publish);
        row.setId(publish.getId());
        mapper.update(row);
    }

    @Override
    public Optional<Publish> findLatestActive(String resourceType, Long resourceId) {
        return mapper.selectListByQuery(
                        QueryWrapper.create()
                                .eq("resource_type", resourceType)
                                .eq("resource_id", resourceId)
                                .eq("status", "PUBLISHED")
                                .orderBy("published_at", false)
                                .limit(1))
                .stream()
                .findFirst()
                .map(this::toDomain);
    }

    @Override
    public void revokeByResource(String resourceType, Long resourceId) {
        mapper.selectListByQuery(
                        QueryWrapper.create()
                                .eq("resource_type", resourceType)
                                .eq("resource_id", resourceId)
                                .eq("status", "PUBLISHED"))
                .forEach(row -> {
                    row.setStatus("REVOKED");
                    mapper.update(row);
                });
    }

    private Publish toDomain(PublishDO row) {
        Publish publish = new Publish();
        publish.setId(row.getId());
        publish.setWorkspaceId(row.getWorkspaceId());
        publish.setResourceType(row.getResourceType());
        publish.setResourceId(row.getResourceId());
        publish.setVersionId(row.getVersionId());
        publish.setChannel(row.getChannel());
        publish.setStatus(row.getStatus());
        publish.setPublishedBy(row.getPublishedBy());
        publish.setPublishedAt(row.getPublishedAt());
        publish.setCreatedAt(row.getCreatedAt());
        return publish;
    }

    private PublishDO toDo(Publish publish) {
        PublishDO row = new PublishDO();
        row.setWorkspaceId(publish.getWorkspaceId());
        row.setResourceType(publish.getResourceType());
        row.setResourceId(publish.getResourceId());
        row.setVersionId(publish.getVersionId());
        row.setChannel(publish.getChannel() == null ? "API" : publish.getChannel());
        row.setStatus(publish.getStatus() == null ? "PUBLISHED" : publish.getStatus());
        row.setPublishedBy(publish.getPublishedBy());
        row.setPublishedAt(publish.getPublishedAt());
        return row;
    }
}
