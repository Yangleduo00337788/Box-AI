package com.boxai.infrastructure.persistence.mapper;

import com.boxai.infrastructure.persistence.entity.AgentVersionDO;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AgentVersionMapper extends BaseMapper<AgentVersionDO> {
}
