package com.boxai.model.application;

import com.boxai.ai.ChatModelGateway;
import com.boxai.ai.ModelRuntimeConfig;
import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.crypto.SecretCipher;
import com.boxai.domain.model.ModelCredential;
import com.boxai.domain.model.ModelCredentialRepository;
import com.boxai.domain.model.ModelDefinition;
import com.boxai.domain.model.ModelDefinitionRepository;
import com.boxai.domain.model.ModelProvider;
import com.boxai.domain.model.ModelProviderRepository;
import com.boxai.model.api.CreateCredentialRequest;
import com.boxai.model.api.CreateModelRequest;
import com.boxai.model.api.CreateProviderRequest;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.permission.WorkspacePermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModelApplicationServiceTest {

    @Mock
    private ModelProviderRepository providerRepository;
    @Mock
    private ModelDefinitionRepository definitionRepository;
    @Mock
    private ModelCredentialRepository credentialRepository;
    @Mock
    private SecretCipher secretCipher;
    @Mock
    private ChatModelGateway chatModelGateway;
    @Mock
    private WorkspacePermissionService workspacePermissionService;

    @InjectMocks
    private ModelApplicationService service;

    @AfterEach
    void tearDown() {
        WorkspaceContext.clear();
    }

    @Test
    void createProviderNormalizesCodeAndDefaultsType() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));

        var vo = service.createProvider(new CreateProviderRequest(" OpenAI ", " OpenAI ", null, "https://api.openai.com"));

        verify(workspacePermissionService).requirePermission(PermissionCodes.MODEL_CREATE);
        ArgumentCaptor<ModelProvider> captor = ArgumentCaptor.forClass(ModelProvider.class);
        verify(providerRepository).save(captor.capture());
        assertEquals("openai", captor.getValue().getProviderCode());
        assertEquals("OPENAI_COMPATIBLE", captor.getValue().getProviderType());
        assertEquals("OpenAI", vo.providerName());
    }

    @Test
    void deleteProviderRejectsOtherWorkspace() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        ModelProvider provider = new ModelProvider();
        provider.setId(2L);
        provider.setWorkspaceId(99L);
        when(providerRepository.findById(2L)).thenReturn(Optional.of(provider));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.deleteProvider(2L));
        assertEquals(ErrorCode.WORKSPACE_ACCESS_DENIED, ex.getCode());
        verify(providerRepository, never()).delete(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void createModelRequiresProviderInWorkspace() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        when(providerRepository.findById(2L)).thenReturn(Optional.of(ownedProvider()));

        service.createModel(new CreateModelRequest(2L, " gpt-4o ", " GPT-4o ", null, true, false, 128000, 4096));

        ArgumentCaptor<ModelDefinition> captor = ArgumentCaptor.forClass(ModelDefinition.class);
        verify(definitionRepository).save(captor.capture());
        assertEquals("gpt-4o", captor.getValue().getModelCode());
        assertEquals("CHAT", captor.getValue().getModelType());
        assertEquals(Boolean.TRUE, captor.getValue().getSupportStreaming());
    }

    @Test
    void createCredentialEncryptsApiKeyAndMasksOnReturn() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        when(providerRepository.findById(2L)).thenReturn(Optional.of(ownedProvider()));
        when(secretCipher.encrypt("sk-abcdefghij")).thenReturn("cipher");
        when(secretCipher.decrypt("cipher")).thenReturn("sk-abcdefghij");

        var vo = service.createCredential(new CreateCredentialRequest(2L, " prod ", "sk-abcdefghij"));

        ArgumentCaptor<ModelCredential> captor = ArgumentCaptor.forClass(ModelCredential.class);
        verify(credentialRepository).save(captor.capture());
        assertEquals("cipher", captor.getValue().getEncryptedApiKey());
        assertEquals("sk-a****ghij", vo.maskedApiKey());
    }

    @Test
    void testChatRejectsMissingCredential() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        ModelDefinition model = ownedModel();
        when(definitionRepository.findById(5L)).thenReturn(Optional.of(model));
        when(providerRepository.findById(2L)).thenReturn(Optional.of(ownedProvider()));
        when(credentialRepository.findActiveByProvider(7L, 2L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.testChat(5L, "hi"));
        assertEquals(ErrorCode.CREDENTIAL_MISSING, ex.getCode());
        verify(chatModelGateway, never()).chat(any(), any());
    }

    @Test
    void testChatDecryptsKeyAndTouchesCredential() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        when(definitionRepository.findById(5L)).thenReturn(Optional.of(ownedModel()));
        when(providerRepository.findById(2L)).thenReturn(Optional.of(ownedProvider()));
        ModelCredential credential = new ModelCredential();
        credential.setId(9L);
        credential.setEncryptedApiKey("cipher");
        when(credentialRepository.findActiveByProvider(7L, 2L)).thenReturn(Optional.of(credential));
        when(secretCipher.decrypt("cipher")).thenReturn("sk-live");
        when(chatModelGateway.chat(any(ModelRuntimeConfig.class), org.mockito.ArgumentMatchers.eq("hello")))
                .thenReturn("pong");

        var vo = service.testChat(5L, "hello");

        assertEquals("pong", vo.content());
        verify(credentialRepository).touchLastUsed(9L);
    }

    private static ModelProvider ownedProvider() {
        ModelProvider provider = new ModelProvider();
        provider.setId(2L);
        provider.setWorkspaceId(7L);
        provider.setProviderName("OpenAI");
        provider.setBaseUrl("https://api.openai.com");
        return provider;
    }

    private static ModelDefinition ownedModel() {
        ModelDefinition model = new ModelDefinition();
        model.setId(5L);
        model.setProviderId(2L);
        model.setModelCode("gpt-4o");
        return model;
    }
}
