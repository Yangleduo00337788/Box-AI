package com.boxai.user.application;

import com.boxai.infrastructure.persistence.entity.OpsPlacementMetricDO;
import com.boxai.infrastructure.persistence.mapper.OpsPlacementMetricMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpsMetricsApplicationServiceTest {

    @Mock
    private OpsPlacementMetricMapper opsPlacementMetricMapper;

    @InjectMocks
    private OpsMetricsApplicationService service;

    @Test
    void trackClickIncrementsClicksOnly() {
        service.track(9L, "CLICK");

        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(opsPlacementMetricMapper).incrementMetric(eq(9L), dateCaptor.capture(), eq(0), eq(1));
        assertEquals(LocalDate.now(), dateCaptor.getValue());
    }

    @Test
    void trackOtherEventsCountAsImpressions() {
        service.track(9L, "view");
        verify(opsPlacementMetricMapper).incrementMetric(eq(9L), org.mockito.ArgumentMatchers.any(), eq(1), eq(0));
    }

    @Test
    void metricsSummaryAggregatesByPlacement() {
        OpsPlacementMetricDO first = row(9L, 10, 2);
        OpsPlacementMetricDO second = row(9L, 3, null);
        OpsPlacementMetricDO other = row(8L, 1, 4);
        when(opsPlacementMetricMapper.selectAll()).thenReturn(List.of(first, second, other));

        var summary = service.metricsSummary();

        assertArrayEquals(new int[] {13, 2}, summary.get(9L));
        assertArrayEquals(new int[] {1, 4}, summary.get(8L));
    }

    private static OpsPlacementMetricDO row(Long placementId, Integer impressions, Integer clicks) {
        OpsPlacementMetricDO row = new OpsPlacementMetricDO();
        row.setPlacementId(placementId);
        row.setImpressions(impressions);
        row.setClicks(clicks);
        return row;
    }
}
