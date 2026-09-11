package com.boxai.infrastructure.persistence.mapper;

import com.boxai.infrastructure.persistence.entity.PublishDO;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PublishMapper extends BaseMapper<PublishDO> {
}
