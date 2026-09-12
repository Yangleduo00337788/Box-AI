package com.boxai.trace.api;

import java.util.List;

public record TraceDetailVO(
        String traceId,
        Long executionId,
        String status,
        List<TraceSpanVO> spans
) {
}
