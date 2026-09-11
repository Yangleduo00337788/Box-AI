package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("plugin_category")
public class PluginCategoryDO {

    @Id
    @Column("category_code")
    private String categoryCode;
    private String label;
    private String description;
    @Column("sort_order")
    private Integer sortOrder;
    private String status;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
