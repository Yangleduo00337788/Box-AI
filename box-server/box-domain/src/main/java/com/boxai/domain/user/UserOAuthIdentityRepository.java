package com.boxai.domain.user;

import java.util.Optional;

public interface UserOAuthIdentityRepository {

    Optional<UserOAuthIdentity> findByProviderAndUserId(String provider, String providerUserId);

    UserOAuthIdentity save(UserOAuthIdentity identity);
}
