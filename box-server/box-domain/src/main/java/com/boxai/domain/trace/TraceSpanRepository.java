package com.boxai.domain.trace;

import java.util.List;

public interface TraceSpanRepository {

    TraceSpan save(TraceSpan span);

    List<TraceSpan> listByTraceId(String traceId);
}
