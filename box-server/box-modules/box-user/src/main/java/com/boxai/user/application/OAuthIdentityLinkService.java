package com.boxai.user.application;

import com.boxai.domain.user.UserOAuthIdentity;
import com.boxai.domain.user.UserOAuthIdentityRepository;
import com.boxai.user.support.OAuthRemoteClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OAuthIdentityLinkService {

    private final UserOAuthIdentityRepository userOAuthIdentityRepository;

    public OAuthIdentityLinkService(UserOAuthIdentityRepository userOAuthIdentityRepository) {
        this.userOAuthIdentityRepository = userOAuthIdentityRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void linkIfAbsent(Long userId, OAuthRemoteClient.OAuthUserProfile profile) {
        if (userOAuthIdentityRepository.findByProviderAndUserId(profile.provider(), profile.providerUserId()).isPresent()) {
            return;
        }
        UserOAuthIdentity identity = new UserOAuthIdentity();
        identity.setUserId(userId);
        identity.setProvider(profile.provider());
        identity.setProviderUserId(profile.providerUserId());
        identity.setEmail(profile.normalizedEmail());
        userOAuthIdentityRepository.save(identity);
    }
}
