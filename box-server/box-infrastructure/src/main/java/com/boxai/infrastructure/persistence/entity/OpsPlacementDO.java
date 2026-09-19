package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("ops_placement")
public class OpsPlacementDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private String audience;
    private String slot;
    private String kind;
    private String title;
    private String body;
    @Column("link_url")
    private String linkUrl;
    @Column("link_label")
    private String linkLabel;
    @Column("icon_name")
    private String iconName;
    @Column("icon_url")
    private String iconUrl;
    @Column("icon_svg")
    private String iconSvg;
    @Column("image_url")
    private String imageUrl;
    private String theme;
    private Integer dismissible;
    private String status;
    @Column("sort_order")
    private Integer sortOrder;
    @Column("starts_at")
    private LocalDateTime startsAt;
    @Column("ends_at")
    private LocalDateTime endsAt;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
    @Column(value = "deleted", isLogicDelete = true)
    private Integer deleted;
}
