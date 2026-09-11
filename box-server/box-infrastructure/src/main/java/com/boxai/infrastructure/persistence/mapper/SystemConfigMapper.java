package com.boxai.infrastructure.persistence.mapper;

import com.boxai.infrastructure.persistence.entity.SystemConfigDO;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfigDO> {}
