package com.boxai.model.application;

import com.boxai.ai.ModelRuntimeConfig;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.crypto.SecretCipher;
import com.boxai.domain.plan.Plan;
import com.boxai.domain.plan.PlanRepository;
import com.boxai.domain.platform.PlatformCredential;
import com.boxai.domain.platform.PlatformCredentialRepository;
import com.boxai.domain.platform.PlatformModel;
import com.boxai.domain.platform.PlatformModelRepository;
import com.boxai.domain.platform.PlatformProvider;
import com.boxai.domain.platform.PlatformProviderRepository;
import com.boxai.domain.tenant.Tenant;
import com.boxai.domain.tenant.TenantRepository;
import com.boxai.model.api.platform.CreatePlatformCredentialRequest;
import com.boxai.model.api.platform.CreatePlatformModelRequest;
import com.boxai.model.api.platform.CreatePlatformProviderRequest;
import com.boxai.model.api.platform.PlatformCredentialVO;
import com.boxai.model.api.platform.PlatformModelVO;
import com.boxai.model.api.platform.PlatformProviderVO;
import com.boxai.model.platform.ResolvedPlatformModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PlatformModelApplicationService {

    private final PlatformProviderRepository providerRepository;
    private final PlatformModelRepository modelRepository;
    private final PlatformCredentialRepository credentialRepository;
    private final TenantRepository tenantRepository;
    private final PlanRepository planRepository;
    private final SecretCipher secretCipher;

    public PlatformModelApplicationService(PlatformProviderRepository providerRepository,
                                           PlatformModelRepository modelRepository,
                                           PlatformCredentialRepository credentialRepository,
                                           TenantRepository tenantRepository,
                                           PlanRepository planRepository,
                                           SecretCipher secretCipher) {
        this.providerRepository = providerRepository;
        this.modelRepository = modelRepository;
        this.credentialRepository = credentialRepository;
        this.tenantRepository = tenantRepository;
        this.planRepository = planRepository;
        this.secretCipher = secretCipher;
    }

    public List<PlatformProviderVO> listProviders() {
        return providerRepository.listAll().stream().map(this::toProviderVO).toList();
    }

    @Transactional
    public PlatformProviderVO createProvider(CreatePlatformProviderRequest request) {
        PlatformProvider provider = new PlatformProvider();
        provider.setProviderCode(request.providerCode().trim().toLowerCase(Locale.ROOT));
        provider.setProviderName(request.providerName().trim());
        provider.setProviderType(request.providerType() == null || request.providerType().isBlank()
                ? "OPENAI_COMPATIBLE" : request.providerType());
        provider.setBaseUrl(request.baseUrl());
        provider.setStatus(1);
        providerRepository.save(provider);
        return toProviderVO(provider);
    }

    public List<PlatformModelVO> listModelsAdmin() {
        Map<Long, PlatformProvider> providerMap = providerRepository.listAll().stream()
                .collect(Collectors.toMap(PlatformProvider::getId, Function.identity()));
        return modelRepository.listAll().stream()
                .map(model -> toModelVO(model, providerMap.get(model.getProviderId())))
                .toList();
    }

    public List<PlatformModelVO> listModelsForConsumer() {
        Set<Long> keyedProviders = credentialRepository.listActiveProviderIds();
        Map<Long, PlatformProvider> providerMap = providerRepository.listAll().stream()
                .filter(item -> item.getStatus() != null && item.getStatus() == 1)
                .filter(item -> keyedProviders.contains(item.getId()))
                .collect(Collectors.toMap(PlatformProvider::getId, Function.identity()));
        return modelRepository.listActive().stream()
                .filter(model -> providerMap.containsKey(model.getProviderId()))
                .map(model -> toModelVO(model, providerMap.get(model.getProviderId())))
                .toList();
    }

    @Transactional
    public PlatformModelVO createModel(CreatePlatformModelRequest request) {
        requireProvider(request.providerId());
        PlatformModel model = new PlatformModel();
        model.setProviderId(request.providerId());
        model.setModelCode(request.modelCode().trim());
        model.setModelName(request.modelName().trim());
        model.setDescription(trimToNull(request.description()));
        model.setModelType("CHAT");
        model.setSupportStreaming(true);
        model.setContextWindow(request.contextWindow());
        model.setMaxOutputTokens(request.maxOutputTokens());
        model.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        model.setStatus(1);
        modelRepository.save(model);
        return toModelVO(model, requireProvider(model.getProviderId()));
    }

    public List<PlatformCredentialVO> listCredentials(Long providerId) {
        requireProvider(providerId);
        return credentialRepository.listByProvider(providerId).stream().map(this::toCredentialVO).toList();
    }

    @Transactional
    public PlatformCredentialVO createCredential(CreatePlatformCredentialRequest request) {
        requireProvider(request.providerId());
        PlatformCredential credential = new PlatformCredential();
        credential.setProviderId(request.providerId());
        credential.setCredentialName(request.credentialName().trim());
        credential.setEncryptedApiKey(secretCipher.encrypt(request.apiKey().trim()));
        credential.setStatus(1);
        credentialRepository.save(credential);
        return toCredentialVO(credential);
    }

    public ResolvedPlatformModel resolveForChat(Long platformModelId) {
        PlatformModel model = modelRepository.findById(platformModelId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLATFORM_MODEL_NOT_FOUND, "平台模型不存在"));
        if (model.getStatus() == null || model.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PLATFORM_MODEL_NOT_FOUND, "平台模型已停用");
        }
        PlatformProvider provider = requireProvider(model.getProviderId());
        if (provider.getStatus() == null || provider.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PLATFORM_PROVIDER_NOT_FOUND, "平台服务商已停用");
        }
        PlatformCredential credential = credentialRepository.findActiveByProvider(provider.getId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.PLATFORM_CREDENTIAL_MISSING,
                        "服务商「" + provider.getProviderName() + "」未配置平台密钥，请到管理端「平台模型池」添加"));
        String apiKey = secretCipher.decrypt(credential.getEncryptedApiKey());
        return new ResolvedPlatformModel(
                new ModelRuntimeConfig(provider.getBaseUrl(), apiKey, model.getModelCode()),
                credential.getId(),
                model.getId());
    }

    public boolean isRunnable(Long platformModelId) {
        if (platformModelId == null) {
            return false;
        }
        Optional<PlatformModel> model = modelRepository.findById(platformModelId);
        if (model.isEmpty() || model.get().getStatus() == null || model.get().getStatus() != 1) {
            return false;
        }
        Optional<PlatformProvider> provider = providerRepository.findById(model.get().getProviderId());
        if (provider.isEmpty() || provider.get().getStatus() == null || provider.get().getStatus() != 1) {
            return false;
        }
        return credentialRepository.findActiveByProvider(provider.get().getId()).isPresent();
    }

    public Optional<Long> findFirstRunnableModelId() {
        return listModelsForConsumer().stream()
                .filter(item -> item.status() != null && item.status() == 1)
                .map(PlatformModelVO::id)
                .filter(Objects::nonNull)
                .findFirst();
    }

    public void touchCredential(Long credentialId) {
        credentialRepository.touchLastUsed(credentialId);
    }

    public boolean isByokEnabledForUser(Long userId) {
        return tenantRepository.findPrimaryByUserId(userId)
                .flatMap(member -> tenantRepository.findById(member.getTenantId()))
                .map(Tenant::getPlanId)
                .flatMap(planRepository::findById)
                .map(Plan::getByokEnabled)
                .map(value -> value != null && value == 1)
                .orElse(false);
    }

    public void assertByokAllowed(Long userId) {
        if (!isByokEnabledForUser(userId)) {
            throw new BusinessException(ErrorCode.BYOK_NOT_ALLOWED, "当前套餐不支持自带密钥，请升级或使用平台模型");
        }
    }

    private PlatformProvider requireProvider(Long id) {
        return providerRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLATFORM_PROVIDER_NOT_FOUND, "平台服务商不存在"));
    }

    private PlatformProviderVO toProviderVO(PlatformProvider provider) {
        return new PlatformProviderVO(
                provider.getId(),
                provider.getProviderCode(),
                provider.getProviderName(),
                provider.getProviderType(),
                provider.getBaseUrl(),
                provider.getStatus());
    }

    private PlatformModelVO toModelVO(PlatformModel model, PlatformProvider provider) {
        return new PlatformModelVO(
                model.getId(),
                model.getProviderId(),
                provider == null ? null : provider.getProviderName(),
                model.getModelCode(),
                model.getModelName(),
                model.getDescription(),
                model.getContextWindow(),
                model.getMaxOutputTokens(),
                model.getSupportStreaming(),
                model.getStatus());
    }

    private PlatformCredentialVO toCredentialVO(PlatformCredential credential) {
        return new PlatformCredentialVO(
                credential.getId(),
                credential.getProviderId(),
                credential.getCredentialName(),
                maskKey(credential.getEncryptedApiKey()),
                credential.getStatus(),
                credential.getLastUsedAt());
    }

    private String maskKey(String encrypted) {
        if (encrypted == null || encrypted.length() < 8) {
            return "****";
        }
        return "sk-****" + encrypted.substring(encrypted.length() - 4);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
