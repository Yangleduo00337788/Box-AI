package com.boxai.domain.trace;

import java.util.Optional;

public interface TraceRepository {

    Trace save(Trace trace);

    void update(Trace trace);

    Optional<Trace> findByExecutionId(Long executionId);
}
