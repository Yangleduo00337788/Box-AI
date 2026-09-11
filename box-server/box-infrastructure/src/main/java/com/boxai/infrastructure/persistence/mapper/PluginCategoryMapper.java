package com.boxai.infrastructure.persistence.mapper;

import com.boxai.infrastructure.persistence.entity.PluginCategoryDO;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PluginCategoryMapper extends BaseMapper<PluginCategoryDO> {}
