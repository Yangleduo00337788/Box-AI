package com.boxai.infrastructure.persistence.mapper;

import com.boxai.infrastructure.persistence.entity.TenantOAuthOrgDO;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TenantOAuthOrgMapper extends BaseMapper<TenantOAuthOrgDO> {}
