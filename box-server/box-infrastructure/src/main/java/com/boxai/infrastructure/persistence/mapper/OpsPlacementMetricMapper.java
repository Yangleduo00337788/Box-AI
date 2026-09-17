package com.boxai.infrastructure.persistence.mapper;

import com.boxai.infrastructure.persistence.entity.OpsPlacementMetricDO;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;

@Mapper
public interface OpsPlacementMetricMapper extends BaseMapper<OpsPlacementMetricDO> {

    @Update("""
            INSERT INTO ops_placement_metric (placement_id, metric_date, impressions, clicks, created_at, updated_at)
            VALUES (#{placementId}, #{metricDate}, #{impressionsInc}, #{clicksInc}, NOW(), NOW())
            ON DUPLICATE KEY UPDATE
              impressions = impressions + VALUES(impressions),
              clicks = clicks + VALUES(clicks),
              updated_at = NOW()
            """)
    void incrementMetric(@Param("placementId") Long placementId,
                         @Param("metricDate") LocalDate metricDate,
                         @Param("impressionsInc") int impressionsInc,
                         @Param("clicksInc") int clicksInc);
}
