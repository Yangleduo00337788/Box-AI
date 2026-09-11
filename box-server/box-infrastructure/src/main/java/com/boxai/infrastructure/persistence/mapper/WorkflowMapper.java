package com.boxai.infrastructure.persistence.mapper;

import com.boxai.infrastructure.persistence.entity.WorkflowDO;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkflowMapper extends BaseMapper<WorkflowDO> {
}
