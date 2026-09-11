package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("platform_provider")
public class PlatformProviderDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column("provider_code")
    private String providerCode;
    @Column("provider_name")
    private String providerName;
    @Column("provider_type")
    private String providerType;
    @Column("base_url")
    private String baseUrl;
    private Integer status;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
    @Column(value = "deleted", isLogicDelete = true)
    private Integer deleted;
}
