package com.boxai.infrastructure.persistence.mapper;

import com.boxai.infrastructure.persistence.entity.ConversationShareDO;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConversationShareMapper extends BaseMapper<ConversationShareDO> {
}
