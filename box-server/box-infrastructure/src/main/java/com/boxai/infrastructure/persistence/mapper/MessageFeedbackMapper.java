package com.boxai.infrastructure.persistence.mapper;

import com.boxai.infrastructure.persistence.entity.MessageFeedbackDO;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageFeedbackMapper extends BaseMapper<MessageFeedbackDO> {
}
