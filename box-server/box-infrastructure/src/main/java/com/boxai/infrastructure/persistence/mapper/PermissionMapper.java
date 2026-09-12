package com.boxai.infrastructure.persistence.mapper;

import com.boxai.infrastructure.persistence.entity.PermissionDO;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PermissionMapper extends BaseMapper<PermissionDO> {
}
