package com.boxai.user.application;

import com.boxai.infrastructure.persistence.entity.OpsPlacementMetricDO;
import com.boxai.infrastructure.persistence.mapper.OpsPlacementMetricMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
        OpsPlacementMetricDO row = opsPlacementMetricMapper.selectOneByQuery(
                QueryWrapper.create().eq("placement_id", placementId).eq("metric_date", today));
        if (row == null) {
            row = new OpsPlacementMetricDO();
            row.setPlacementId(placementId);
            row.setMetricDate(today);
            row.setImpressions(0);
            row.setClicks(0);
            row.setCreatedAt(LocalDateTime.now());
            row.setUpdatedAt(LocalDateTime.now());
            opsPlacementMetricMapper.insert(row);
        }
        if ("click".equalsIgnoreCase(event)) {
            row.setClicks((row.getClicks() == null ? 0 : row.getClicks()) + 1);
        } else {
            row.setImpressions((row.getImpressions() == null ? 0 : row.getImpressions()) + 1);
        }
        row.setUpdatedAt(LocalDateTime.now());
        opsPlacementMetricMapper.update(row);
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
