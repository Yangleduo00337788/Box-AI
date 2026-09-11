package com.boxai.infrastructure.persistence.mapper;

import com.boxai.infrastructure.persistence.entity.KnowledgeChunkDO;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface KnowledgeChunkMapper extends BaseMapper<KnowledgeChunkDO> {
}
