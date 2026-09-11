package com.boxai.infrastructure.persistence.mapper;

import com.boxai.infrastructure.persistence.entity.ApiKeyDO;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ApiKeyMapper extends BaseMapper<ApiKeyDO> {
}
