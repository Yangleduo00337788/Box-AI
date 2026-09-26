package com.boxai.integration;

import com.boxai.domain.crypto.SecretCipher;
import com.boxai.domain.platform.PlatformCredential;
import com.boxai.domain.platform.PlatformCredentialRepository;
import com.boxai.domain.platform.PlatformProviderRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("integration")
class IntegrationTestPlatformModelSeeder {

    IntegrationTestPlatformModelSeeder(PlatformCredentialRepository credentialRepository,
                                       PlatformProviderRepository providerRepository,
                                       SecretCipher secretCipher) {
        if (!credentialRepository.listActiveProviderIds().isEmpty()) {
            return;
        }
        providerRepository.findByCode("openai").ifPresent(provider -> {
            PlatformCredential credential = new PlatformCredential();
            credential.setProviderId(provider.getId());
            credential.setCredentialName("integration-test");
            credential.setEncryptedApiKey(secretCipher.encrypt("sk-integration-test-key"));
            credential.setStatus(1);
            credentialRepository.save(credential);
        });
    }
}
