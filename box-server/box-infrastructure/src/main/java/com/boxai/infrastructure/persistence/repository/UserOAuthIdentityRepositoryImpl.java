package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.user.UserOAuthIdentity;
import com.boxai.domain.user.UserOAuthIdentityRepository;
import com.boxai.infrastructure.persistence.entity.UserOAuthIdentityDO;
import com.boxai.infrastructure.persistence.mapper.UserOAuthIdentityMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class UserOAuthIdentityRepositoryImpl implements UserOAuthIdentityRepository {

    private final UserOAuthIdentityMapper mapper;

    public UserOAuthIdentityRepositoryImpl(UserOAuthIdentityMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<UserOAuthIdentity> findByProviderAndUserId(String provider, String providerUserId) {
        UserOAuthIdentityDO row = mapper.selectOneByQuery(QueryWrapper.create()
                .eq("provider", provider)
                .eq("provider_user_id", providerUserId));
        return Optional.ofNullable(row).map(this::toDomain);
    }

    @Override
    public UserOAuthIdentity save(UserOAuthIdentity identity) {
        UserOAuthIdentityDO row = new UserOAuthIdentityDO();
        row.setUserId(identity.getUserId());
        row.setProvider(identity.getProvider());
        row.setProviderUserId(identity.getProviderUserId());
        row.setEmail(identity.getEmail());
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(row.getCreatedAt());
        mapper.insert(row);
        identity.setId(row.getId());
        identity.setCreatedAt(row.getCreatedAt());
        return identity;
    }

    private UserOAuthIdentity toDomain(UserOAuthIdentityDO row) {
        UserOAuthIdentity identity = new UserOAuthIdentity();
        identity.setId(row.getId());
        identity.setUserId(row.getUserId());
        identity.setProvider(row.getProvider());
        identity.setProviderUserId(row.getProviderUserId());
        identity.setEmail(row.getEmail());
        identity.setCreatedAt(row.getCreatedAt());
        return identity;
    }
}
