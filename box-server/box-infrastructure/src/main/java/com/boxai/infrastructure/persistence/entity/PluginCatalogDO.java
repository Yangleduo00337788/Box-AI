package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("plugin_catalog")
public class PluginCatalogDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("plugin_code")
    private String pluginCode;
    private String category;
    private String title;
    private String description;
    private String status;
    @Column("sort_order")
    private Integer sortOrder;
    @Column("install_count")
    private Integer installCount;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
    @Column(value = "deleted", isLogicDelete = true)
    private Integer deleted;
}
