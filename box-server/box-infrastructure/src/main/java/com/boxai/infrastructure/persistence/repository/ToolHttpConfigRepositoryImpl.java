package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.tool.ToolHttpConfig;
import com.boxai.domain.tool.ToolHttpConfigRepository;
import com.boxai.infrastructure.persistence.entity.ToolHttpConfigDO;
import com.boxai.infrastructure.persistence.mapper.ToolHttpConfigMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class ToolHttpConfigRepositoryImpl implements ToolHttpConfigRepository {

    private final ToolHttpConfigMapper mapper;

    public ToolHttpConfigRepositoryImpl(ToolHttpConfigMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ToolHttpConfig save(ToolHttpConfig config) {
        ToolHttpConfigDO row = toDo(config);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        mapper.insert(row);
        config.setId(row.getId());
        config.setCreatedAt(row.getCreatedAt());
        config.setUpdatedAt(row.getUpdatedAt());
        return config;
    }

    @Override
    public void update(ToolHttpConfig config) {
        ToolHttpConfigDO row = toDo(config);
        row.setId(config.getId());
        row.setUpdatedAt(LocalDateTime.now());
        mapper.update(row);
        config.setUpdatedAt(row.getUpdatedAt());
    }

    @Override
    public Optional<ToolHttpConfig> findByToolId(Long toolId) {
        return Optional.ofNullable(
                        mapper.selectOneByQuery(QueryWrapper.create().eq("tool_id", toolId)))
                .map(this::toDomain);
    }

    @Override
    public void deleteByToolId(Long toolId) {
        mapper.deleteByQuery(QueryWrapper.create().eq("tool_id", toolId));
    }

    private ToolHttpConfig toDomain(ToolHttpConfigDO row) {
        ToolHttpConfig config = new ToolHttpConfig();
        config.setId(row.getId());
        config.setToolId(row.getToolId());
        config.setMethod(row.getMethod());
        config.setUrl(row.getUrl());
        config.setHeadersJson(row.getHeaders());
        config.setQueryParamsJson(row.getQueryParams());
        config.setBodyType(row.getBodyType());
        config.setBodyTemplate(row.getBodyTemplate());
        config.setTimeoutMs(row.getTimeoutMs());
        config.setAllowRedirect(row.getAllowRedirect() != null && row.getAllowRedirect() == 1);
        config.setCreatedAt(row.getCreatedAt());
        config.setUpdatedAt(row.getUpdatedAt());
        return config;
    }

    private ToolHttpConfigDO toDo(ToolHttpConfig config) {
        ToolHttpConfigDO row = new ToolHttpConfigDO();
        row.setToolId(config.getToolId());
        row.setMethod(config.getMethod());
        row.setUrl(config.getUrl());
        row.setHeaders(config.getHeadersJson());
        row.setQueryParams(config.getQueryParamsJson());
        row.setBodyType(config.getBodyType());
        row.setBodyTemplate(config.getBodyTemplate());
        row.setTimeoutMs(config.getTimeoutMs());
        row.setAllowRedirect(Boolean.TRUE.equals(config.getAllowRedirect()) ? 1 : 0);
        return row;
    }
}
