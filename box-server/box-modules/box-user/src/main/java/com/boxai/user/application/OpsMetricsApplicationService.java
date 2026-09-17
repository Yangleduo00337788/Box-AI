package com.boxai.user.application;

import com.boxai.infrastructure.persistence.mapper.OpsPlacementMetricMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
public class OpsMetricsApplicationService {

    private final OpsPlacementMetricMapper opsPlacementMetricMapper;

    public OpsMetricsApplicationService(OpsPlacementMetricMapper opsPlacementMetricMapper) {
        this.opsPlacementMetricMapper = opsPlacementMetricMapper;
    }

    @Transactional
    public void track(Long placementId, String event) {
        LocalDate today = LocalDate.now();
        boolean click = "click".equalsIgnoreCase(event);
        opsPlacementMetricMapper.incrementMetric(
                placementId,
                today,
                click ? 0 : 1,
                click ? 1 : 0);
    }

    public Map<Long, int[]> metricsSummary() {
        Map<Long, int[]> summary = new HashMap<>();
        opsPlacementMetricMapper.selectAll().forEach(row -> {
            int[] values = summary.computeIfAbsent(row.getPlacementId(), key -> new int[] {0, 0});
            values[0] += row.getImpressions() == null ? 0 : row.getImpressions();
            values[1] += row.getClicks() == null ? 0 : row.getClicks();
        });
        return summary;
    }
}
