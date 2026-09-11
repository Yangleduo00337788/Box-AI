package com.boxai.infrastructure.persistence.mapper;

import com.boxai.infrastructure.persistence.entity.AgentKnowledgeDO;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AgentKnowledgeMapper extends BaseMapper<AgentKnowledgeDO> {
}
