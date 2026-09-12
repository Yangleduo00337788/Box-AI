package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.trace.TraceSpan;
import com.boxai.domain.trace.TraceSpanRepository;
import com.boxai.infrastructure.persistence.entity.TraceSpanDO;
import com.boxai.infrastructure.persistence.mapper.TraceSpanMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class TraceSpanRepositoryImpl implements TraceSpanRepository {

    private final TraceSpanMapper mapper;

    public TraceSpanRepositoryImpl(TraceSpanMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public TraceSpan save(TraceSpan span) {
        TraceSpanDO row = toDo(span);
        row.setCreatedAt(LocalDateTime.now());
        mapper.insert(row);
        span.setId(row.getId());
        span.setCreatedAt(row.getCreatedAt());
        return span;
    }

    @Override
    public List<TraceSpan> listByTraceId(String traceId) {
        return mapper.selectListByQuery(
                        QueryWrapper.create()
                                .eq("trace_id", traceId)
                                .orderBy("start_time", true))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private TraceSpan toDomain(TraceSpanDO row) {
        TraceSpan span = new TraceSpan();
        span.setId(row.getId());
        span.setTraceId(row.getTraceId());
        span.setSpanId(row.getSpanId());
        span.setParentSpanId(row.getParentSpanId());
        span.setSpanType(row.getSpanType());
        span.setName(row.getName());
        span.setStatus(row.getStatus());
        span.setInputJson(row.getInput());
        span.setOutputJson(row.getOutput());
        span.setModelId(row.getModelId());
        span.setToolId(row.getToolId());
        span.setInputTokens(row.getInputTokens());
        span.setOutputTokens(row.getOutputTokens());
        span.setStartTime(row.getStartTime());
        span.setEndTime(row.getEndTime());
        span.setDurationMs(row.getDurationMs());
        span.setErrorCode(row.getErrorCode());
        span.setErrorMessage(row.getErrorMessage());
        span.setMetadataJson(row.getMetadata());
        span.setCreatedAt(row.getCreatedAt());
        return span;
    }

    private TraceSpanDO toDo(TraceSpan span) {
        TraceSpanDO row = new TraceSpanDO();
        row.setTraceId(span.getTraceId());
        row.setSpanId(span.getSpanId());
        row.setParentSpanId(span.getParentSpanId());
        row.setSpanType(span.getSpanType());
        row.setName(span.getName());
        row.setStatus(span.getStatus());
        row.setInput(span.getInputJson());
        row.setOutput(span.getOutputJson());
        row.setModelId(span.getModelId());
        row.setToolId(span.getToolId());
        row.setInputTokens(span.getInputTokens());
        row.setOutputTokens(span.getOutputTokens());
        row.setStartTime(span.getStartTime());
        row.setEndTime(span.getEndTime());
        row.setDurationMs(span.getDurationMs());
        row.setErrorCode(span.getErrorCode());
        row.setErrorMessage(span.getErrorMessage());
        row.setMetadata(span.getMetadataJson());
        return row;
    }
}
