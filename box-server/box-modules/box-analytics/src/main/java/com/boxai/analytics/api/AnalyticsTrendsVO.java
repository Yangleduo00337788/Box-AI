package com.boxai.analytics.api;

import java.util.List;

public record AnalyticsTrendsVO(
        int periodDays,
        List<AnalyticsTrendPointVO> points
) {
}
