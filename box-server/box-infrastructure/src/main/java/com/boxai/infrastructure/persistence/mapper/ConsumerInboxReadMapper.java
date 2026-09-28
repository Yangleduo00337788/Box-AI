package com.boxai.infrastructure.persistence.mapper;

import com.boxai.infrastructure.persistence.entity.ConsumerInboxReadDO;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConsumerInboxReadMapper extends BaseMapper<ConsumerInboxReadDO> {
}
