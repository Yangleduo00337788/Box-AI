package com.boxai.infrastructure.persistence.mapper;

import com.boxai.infrastructure.persistence.entity.SubscriptionDO;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SubscriptionMapper extends BaseMapper<SubscriptionDO> {
}
