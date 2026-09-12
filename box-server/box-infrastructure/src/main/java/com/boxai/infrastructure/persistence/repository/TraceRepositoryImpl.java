package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.trace.Trace;
import com.boxai.domain.trace.TraceRepository;
import com.boxai.infrastructure.persistence.entity.TraceDO;
import com.boxai.infrastructure.persistence.mapper.TraceMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class TraceRepositoryImpl implements TraceRepository {

    private final TraceMapper mapper;

    public TraceRepositoryImpl(TraceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Trace save(Trace trace) {
        TraceDO row = toDo(trace);
        row.setCreatedAt(LocalDateTime.now());
        mapper.insert(row);
        trace.setId(row.getId());
        trace.setCreatedAt(row.getCreatedAt());
        return trace;
    }

    @Override
    public void update(Trace trace) {
        TraceDO row = toDo(trace);
        row.setId(trace.getId());
        mapper.update(row);
    }

    @Override
    public Optional<Trace> findByExecutionId(Long executionId) {
        return Optional.ofNullable(mapper.selectOneByQuery(
                        QueryWrapper.create().eq("execution_id", executionId)))
                .map(this::toDomain);
    }

    private Trace toDomain(TraceDO row) {
        Trace trace = new Trace();
        trace.setId(row.getId());
        trace.setTraceId(row.getTraceId());
        trace.setExecutionId(row.getExecutionId());
        trace.setWorkspaceId(row.getWorkspaceId());
        trace.setName(row.getName());
        trace.setStatus(row.getStatus());
        trace.setStartTime(row.getStartTime());
        trace.setEndTime(row.getEndTime());
        trace.setDurationMs(row.getDurationMs());
        trace.setMetadataJson(row.getMetadata());
        trace.setCreatedAt(row.getCreatedAt());
        return trace;
    }

    private TraceDO toDo(Trace trace) {
        TraceDO row = new TraceDO();
        row.setTraceId(trace.getTraceId());
        row.setExecutionId(trace.getExecutionId());
        row.setWorkspaceId(trace.getWorkspaceId());
        row.setName(trace.getName());
        row.setStatus(trace.getStatus());
        row.setStartTime(trace.getStartTime());
        row.setEndTime(trace.getEndTime());
        row.setDurationMs(trace.getDurationMs());
        row.setMetadata(trace.getMetadataJson());
        return row;
    }
}
