package com.boxai.model.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.crypto.SecretCipher;
import com.boxai.domain.plan.PlanRepository;
import com.boxai.domain.platform.PlatformCredentialRepository;
import com.boxai.domain.platform.PlatformModel;
import com.boxai.domain.platform.PlatformModelRepository;
import com.boxai.domain.platform.PlatformProvider;
import com.boxai.domain.platform.PlatformProviderRepository;
import com.boxai.domain.tenant.TenantRepository;
import com.boxai.model.api.platform.CreatePlatformModelRequest;
import com.boxai.model.api.platform.CreatePlatformProviderRequest;
import com.boxai.model.api.platform.UpdatePlatformModelRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformModelApplicationServiceTest {

    @Mock
    private PlatformProviderRepository providerRepository;
    @Mock
    private PlatformModelRepository modelRepository;
    @Mock
    private PlatformCredentialRepository credentialRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private PlanRepository planRepository;
    @Mock
    private SecretCipher secretCipher;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PlatformModelApplicationService service;

    @Test
    void createProviderRejectsDuplicateCode() {
        when(providerRepository.findByCode("openai")).thenReturn(Optional.of(new PlatformProvider()));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.createProvider(
                new CreatePlatformProviderRequest("OpenAI", "OpenAI", null, null)));
        assertEquals(ErrorCode.CONFLICT, ex.getCode());
        verify(providerRepository, never()).save(any());
    }

    @Test
    void createProviderNormalizesCodeAndDefaultsType() {
        when(providerRepository.findByCode("openai")).thenReturn(Optional.empty());

        service.createProvider(new CreatePlatformProviderRequest(" OpenAI ", " OpenAI Inc ", "  ", " https://api.openai.com/ "));

        ArgumentCaptor<PlatformProvider> captor = ArgumentCaptor.forClass(PlatformProvider.class);
        verify(providerRepository).save(captor.capture());
        assertEquals("openai", captor.getValue().getProviderCode());
        assertEquals("OpenAI Inc", captor.getValue().getProviderName());
        assertEquals("OPENAI_COMPATIBLE", captor.getValue().getProviderType());
        assertEquals("https://api.openai.com/", captor.getValue().getBaseUrl());
        assertEquals(1, captor.getValue().getStatus());
    }

    @Test
    void createModelRejectsDuplicateLiveCode() {
        when(providerRepository.findById(2L)).thenReturn(Optional.of(provider(2L)));
        PlatformModel existing = new PlatformModel();
        existing.setDeleted(0);
        when(modelRepository.findByProviderAndCodeIncludingDeleted(2L, "gpt-4.1")).thenReturn(Optional.of(existing));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createModel(
                new CreatePlatformModelRequest(2L, "gpt-4.1", "GPT", null, null, null, null)));
        assertEquals(ErrorCode.CONFLICT, ex.getCode());
        verify(modelRepository, never()).save(any());
    }

    @Test
    void listModelsForConsumerKeepsChatModelsWithActiveCredentials() {
        when(credentialRepository.listActiveProviderIds()).thenReturn(Set.of(2L));
        PlatformProvider provider = provider(2L);
        provider.setStatus(1);
        when(providerRepository.listAll()).thenReturn(List.of(provider));
        PlatformModel chat = model(11L, 2L, "CHAT");
        PlatformModel embedding = model(12L, 2L, "EMBEDDING");
        PlatformModel otherProvider = model(13L, 9L, "CHAT");
        when(modelRepository.listActive()).thenReturn(List.of(chat, embedding, otherProvider));

        var vos = service.listModelsForConsumer();

        assertEquals(1, vos.size());
        assertEquals("gpt-chat", vos.get(0).modelCode());
        assertEquals("OpenAI", vos.get(0).providerName());
    }

    @Test
    void updateModelTogglesStatus() {
        PlatformModel model = model(11L, 2L, "CHAT");
        when(modelRepository.findById(11L)).thenReturn(Optional.of(model));
        when(providerRepository.findById(2L)).thenReturn(Optional.of(provider(2L)));

        var vo = service.updateModel(11L, new UpdatePlatformModelRequest(" GPT-4 ", null, 128000, null, null, 0));

        verify(modelRepository).update(model);
        assertEquals("GPT-4", model.getModelName());
        assertEquals(128000, model.getContextWindow());
        assertEquals(0, model.getStatus());
        assertEquals("GPT-4", vo.modelName());
    }

    @Test
    void deleteProviderRemovesCredentialsAndModels() {
        when(providerRepository.findById(2L)).thenReturn(Optional.of(provider(2L)));
        service.deleteProvider(2L);
        verify(credentialRepository).deleteByProvider(2L);
        verify(modelRepository).deleteByProvider(2L);
        verify(providerRepository).delete(2L);
    }

    private static PlatformProvider provider(Long id) {
        PlatformProvider provider = new PlatformProvider();
        provider.setId(id);
        provider.setProviderCode("openai");
        provider.setProviderName("OpenAI");
        provider.setProviderType("OPENAI_COMPATIBLE");
        provider.setStatus(1);
        return provider;
    }

    private static PlatformModel model(Long id, Long providerId, String type) {
        PlatformModel model = new PlatformModel();
        model.setId(id);
        model.setProviderId(providerId);
        model.setModelCode("gpt-chat");
        model.setModelName("GPT");
        model.setModelType(type);
        model.setStatus(1);
        model.setSupportStreaming(true);
        return model;
    }
}
