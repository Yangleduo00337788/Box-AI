package com.boxai.infrastructure.persistence.mapper;

import com.boxai.infrastructure.persistence.entity.UserSessionDO;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserSessionMapper extends BaseMapper<UserSessionDO> {
}
