package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.tool.ToolFunctionConfig;
import com.boxai.domain.tool.ToolFunctionConfigRepository;
import com.boxai.infrastructure.persistence.entity.ToolFunctionConfigDO;
import com.boxai.infrastructure.persistence.mapper.ToolFunctionConfigMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class ToolFunctionConfigRepositoryImpl implements ToolFunctionConfigRepository {

    private final ToolFunctionConfigMapper mapper;

    public ToolFunctionConfigRepositoryImpl(ToolFunctionConfigMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ToolFunctionConfig save(ToolFunctionConfig config) {
        ToolFunctionConfigDO row = toDo(config);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        mapper.insert(row);
        config.setId(row.getId());
        config.setCreatedAt(row.getCreatedAt());
        config.setUpdatedAt(row.getUpdatedAt());
        return config;
    }

    @Override
    public void update(ToolFunctionConfig config) {
        ToolFunctionConfigDO row = toDo(config);
        row.setId(config.getId());
        row.setUpdatedAt(LocalDateTime.now());
        mapper.update(row);
        config.setUpdatedAt(row.getUpdatedAt());
    }

    @Override
    public Optional<ToolFunctionConfig> findByToolId(Long toolId) {
        return Optional.ofNullable(
                        mapper.selectOneByQuery(QueryWrapper.create().eq("tool_id", toolId)))
                .map(this::toDomain);
    }

    @Override
    public void deleteByToolId(Long toolId) {
        mapper.deleteByQuery(QueryWrapper.create().eq("tool_id", toolId));
    }

    private ToolFunctionConfig toDomain(ToolFunctionConfigDO row) {
        ToolFunctionConfig config = new ToolFunctionConfig();
        config.setId(row.getId());
        config.setToolId(row.getToolId());
        config.setFunctionName(row.getFunctionName());
        config.setFunctionCode(row.getFunctionCode());
        config.setRuntime(row.getRuntime());
        config.setTimeoutMs(row.getTimeoutMs());
        config.setMemoryLimitMb(row.getMemoryLimitMb());
        config.setCreatedAt(row.getCreatedAt());
        config.setUpdatedAt(row.getUpdatedAt());
        return config;
    }

    private ToolFunctionConfigDO toDo(ToolFunctionConfig config) {
        ToolFunctionConfigDO row = new ToolFunctionConfigDO();
        row.setToolId(config.getToolId());
        row.setFunctionName(config.getFunctionName());
        row.setFunctionCode(config.getFunctionCode());
        row.setRuntime(config.getRuntime());
        row.setTimeoutMs(config.getTimeoutMs());
        row.setMemoryLimitMb(config.getMemoryLimitMb());
        return row;
    }
}
