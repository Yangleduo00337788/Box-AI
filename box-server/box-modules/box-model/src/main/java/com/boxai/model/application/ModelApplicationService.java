package com.boxai.model.application;

import com.boxai.ai.ChatModelGateway;
import com.boxai.ai.ModelRuntimeConfig;
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
import com.boxai.model.api.CredentialVO;
import com.boxai.model.api.ModelVO;
import com.boxai.model.api.ProviderVO;
import com.boxai.model.api.TestChatVO;
import com.boxai.security.context.WorkspaceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ModelApplicationService {

    private final ModelProviderRepository providerRepository;
    private final ModelDefinitionRepository definitionRepository;
    private final ModelCredentialRepository credentialRepository;
    private final SecretCipher secretCipher;
    private final ChatModelGateway chatModelGateway;

    public ModelApplicationService(ModelProviderRepository providerRepository,
                                   ModelDefinitionRepository definitionRepository,
                                   ModelCredentialRepository credentialRepository,
                                   SecretCipher secretCipher,
                                   ChatModelGateway chatModelGateway) {
        this.providerRepository = providerRepository;
        this.definitionRepository = definitionRepository;
        this.credentialRepository = credentialRepository;
        this.secretCipher = secretCipher;
        this.chatModelGateway = chatModelGateway;
    }

    public List<ProviderVO> listProviders() {
        return providerRepository.listByWorkspace(workspaceId()).stream().map(this::toProviderVO).toList();
    }

    @Transactional
    public ProviderVO createProvider(CreateProviderRequest request) {
        ModelProvider provider = new ModelProvider();
        provider.setWorkspaceId(workspaceId());
        provider.setProviderCode(request.providerCode().trim().toLowerCase(Locale.ROOT));
        provider.setProviderName(request.providerName().trim());
        provider.setProviderType(request.providerType() == null || request.providerType().isBlank()
                ? "OPENAI_COMPATIBLE" : request.providerType());
        provider.setBaseUrl(request.baseUrl());
        provider.setStatus(1);
        providerRepository.save(provider);
        return toProviderVO(provider);
    }

    @Transactional
    public ProviderVO updateProvider(Long id, CreateProviderRequest request) {
        ModelProvider provider = requireProvider(id);
        provider.setProviderCode(request.providerCode().trim().toLowerCase(Locale.ROOT));
        provider.setProviderName(request.providerName().trim());
        provider.setProviderType(request.providerType() == null || request.providerType().isBlank()
                ? provider.getProviderType() : request.providerType());
        provider.setBaseUrl(request.baseUrl());
        providerRepository.update(provider);
        return toProviderVO(provider);
    }

    @Transactional
    public void deleteProvider(Long id) {
        requireProvider(id);
        providerRepository.delete(id);
    }

    public List<ModelVO> listModels() {
        List<ModelProvider> providers = providerRepository.listByWorkspace(workspaceId());
        Map<Long, ModelProvider> providerMap = providers.stream()
                .collect(Collectors.toMap(ModelProvider::getId, Function.identity()));
        return definitionRepository.listByProviderIds(providers.stream().map(ModelProvider::getId).toList())
                .stream()
                .map(item -> toModelVO(item, providerMap.get(item.getProviderId())))
                .toList();
    }

    @Transactional
    public ModelVO createModel(CreateModelRequest request) {
        ModelProvider provider = requireProvider(request.providerId());
        ModelDefinition model = new ModelDefinition();
        model.setProviderId(provider.getId());
        model.setModelCode(request.modelCode().trim());
        model.setModelName(request.modelName().trim());
        model.setModelType(request.modelType() == null ? "CHAT" : request.modelType());
        model.setSupportStreaming(Boolean.TRUE.equals(request.supportStreaming()));
        model.setSupportToolCalling(Boolean.TRUE.equals(request.supportToolCalling()));
        model.setContextWindow(request.contextWindow());
        model.setMaxOutputTokens(request.maxOutputTokens());
        model.setStatus(1);
        definitionRepository.save(model);
        return toModelVO(model, provider);
    }

    @Transactional
    public ModelVO updateModel(Long id, CreateModelRequest request) {
        ModelDefinition model = definitionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MODEL_NOT_FOUND, "模型不存在"));
        ModelProvider provider = requireProvider(model.getProviderId());
        model.setModelCode(request.modelCode().trim());
        model.setModelName(request.modelName().trim());
        model.setModelType(request.modelType() == null ? model.getModelType() : request.modelType());
        model.setSupportStreaming(Boolean.TRUE.equals(request.supportStreaming()));
        model.setSupportToolCalling(Boolean.TRUE.equals(request.supportToolCalling()));
        model.setContextWindow(request.contextWindow());
        model.setMaxOutputTokens(request.maxOutputTokens());
        definitionRepository.update(model);
        return toModelVO(model, provider);
    }

    @Transactional
    public void deleteModel(Long id) {
        ModelDefinition model = definitionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MODEL_NOT_FOUND, "模型不存在"));
        requireProvider(model.getProviderId());
        definitionRepository.delete(id);
    }

    public List<CredentialVO> listCredentials() {
        List<ModelProvider> providers = providerRepository.listByWorkspace(workspaceId());
        Map<Long, ModelProvider> providerMap = providers.stream()
                .collect(Collectors.toMap(ModelProvider::getId, Function.identity()));
        return credentialRepository.listByWorkspace(workspaceId()).stream()
                .map(item -> toCredentialVO(item, providerMap.get(item.getProviderId())))
                .toList();
    }

    @Transactional
    public CredentialVO createCredential(CreateCredentialRequest request) {
        ModelProvider provider = requireProvider(request.providerId());
        ModelCredential credential = new ModelCredential();
        credential.setWorkspaceId(workspaceId());
        credential.setProviderId(provider.getId());
        credential.setCredentialName(request.credentialName().trim());
        credential.setEncryptedApiKey(secretCipher.encrypt(request.apiKey().trim()));
        credential.setStatus(1);
        credentialRepository.save(credential);
        return toCredentialVO(credential, provider);
    }

    @Transactional
    public void deleteCredential(Long id) {
        ModelCredential credential = credentialRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREDENTIAL_NOT_FOUND, "密钥不存在"));
        if (!workspaceId().equals(credential.getWorkspaceId())) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "无权访问该密钥");
        }
        credentialRepository.delete(id);
    }

    public TestChatVO testChat(Long modelId, String message) {
        ModelDefinition model = definitionRepository.findById(modelId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MODEL_NOT_FOUND, "模型不存在"));
        ModelProvider provider = requireProvider(model.getProviderId());
        ModelCredential credential = credentialRepository.findActiveByProvider(workspaceId(), provider.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CREDENTIAL_MISSING, "请先为该 Provider 配置 API Key"));
        String apiKey = secretCipher.decrypt(credential.getEncryptedApiKey());
        try {
            String content = chatModelGateway.chat(
                    new ModelRuntimeConfig(provider.getBaseUrl(), apiKey, model.getModelCode()),
                    message);
            credentialRepository.touchLastUsed(credential.getId());
            return new TestChatVO(content);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.EXECUTION_FAILED, "模型调用失败");
        }
    }

    private ModelProvider requireProvider(Long providerId) {
        ModelProvider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROVIDER_NOT_FOUND, "Provider 不存在"));
        if (!workspaceId().equals(provider.getWorkspaceId())) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "无权访问该 Provider");
        }
        return provider;
    }

    private Long workspaceId() {
        return WorkspaceContext.require().workspaceId();
    }

    private ProviderVO toProviderVO(ModelProvider provider) {
        return new ProviderVO(
                provider.getId(),
                provider.getProviderCode(),
                provider.getProviderName(),
                provider.getProviderType(),
                provider.getBaseUrl(),
                provider.getStatus());
    }

    private ModelVO toModelVO(ModelDefinition model, ModelProvider provider) {
        return new ModelVO(
                model.getId(),
                model.getProviderId(),
                provider == null ? null : provider.getProviderName(),
                model.getModelCode(),
                model.getModelName(),
                model.getModelType(),
                model.getSupportStreaming(),
                model.getSupportToolCalling(),
                model.getStatus());
    }

    private CredentialVO toCredentialVO(ModelCredential credential, ModelProvider provider) {
        return new CredentialVO(
                credential.getId(),
                credential.getProviderId(),
                provider == null ? null : provider.getProviderName(),
                credential.getCredentialName(),
                mask(secretCipher.decrypt(credential.getEncryptedApiKey())),
                credential.getStatus());
    }

    private String mask(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
