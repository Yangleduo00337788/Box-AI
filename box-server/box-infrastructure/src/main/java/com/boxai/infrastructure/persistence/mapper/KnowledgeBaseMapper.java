package com.boxai.infrastructure.persistence.mapper;

import com.boxai.infrastructure.persistence.entity.KnowledgeBaseDO;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBaseDO> {
}
