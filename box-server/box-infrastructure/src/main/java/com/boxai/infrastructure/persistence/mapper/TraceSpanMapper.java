package com.boxai.infrastructure.persistence.mapper;

import com.boxai.infrastructure.persistence.entity.TraceSpanDO;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TraceSpanMapper extends BaseMapper<TraceSpanDO> {
}
