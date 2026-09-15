package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Table("ops_placement_metric")
public class OpsPlacementMetricDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("placement_id")
    private Long placementId;
    @Column("metric_date")
    private LocalDate metricDate;
    private Integer impressions;
    private Integer clicks;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
