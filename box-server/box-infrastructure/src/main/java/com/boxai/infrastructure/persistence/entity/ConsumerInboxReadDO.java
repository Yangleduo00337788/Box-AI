package com.boxai.infrastructure.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("consumer_inbox_read")
public class ConsumerInboxReadDO {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long userId;
    private Long workspaceId;
    private String noticeKey;
    private LocalDateTime createdAt;
}
