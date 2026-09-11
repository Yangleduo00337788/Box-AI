package com.boxai.infrastructure.persistence.mapper;

import com.boxai.infrastructure.persistence.entity.UserDO;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<UserDO> {}
