package com.boxai.infrastructure.persistence.mapper;

import com.boxai.infrastructure.persistence.entity.AgentTemplateDO;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AgentTemplateMapper extends BaseMapper<AgentTemplateDO> {
}
