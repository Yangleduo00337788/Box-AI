package com.boxai.infrastructure.persistence.mapper;

import com.boxai.infrastructure.persistence.entity.AgentWorkflowDO;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AgentWorkflowMapper extends BaseMapper<AgentWorkflowDO> {
}
