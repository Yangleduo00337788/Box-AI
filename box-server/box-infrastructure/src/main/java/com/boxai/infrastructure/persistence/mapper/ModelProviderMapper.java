package com.boxai.infrastructure.persistence.mapper;

import com.boxai.infrastructure.persistence.entity.ModelProviderDO;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ModelProviderMapper extends BaseMapper<ModelProviderDO> {}
