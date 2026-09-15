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
import com.boxai.common.security.SsrfGuard;
import com.boxai.common.security.SsrfSafeHttpClient;
import com.boxai.model.api.platform.CreatePlatformCredentialRequest;
import com.boxai.model.api.platform.CreatePlatformModelRequest;
import com.boxai.model.api.platform.CreatePlatformProviderRequest;
import com.boxai.model.api.platform.ImportPlatformModelsRequest;
import com.boxai.model.api.platform.PlatformCredentialVO;
import com.boxai.model.api.platform.PlatformModelVO;
import com.boxai.model.api.platform.PlatformProviderVO;
import com.boxai.model.api.platform.UpdatePlatformModelRequest;
import com.boxai.model.api.platform.UpdatePlatformProviderRequest;
import com.boxai.model.api.platform.UpstreamModelVO;
import com.boxai.model.platform.ResolvedPlatformModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = SsrfSafeHttpClient.create(Duration.ofSeconds(5), false);

    public PlatformModelApplicationService(PlatformProviderRepository providerRepository,
                                           PlatformModelRepository modelRepository,
                                           PlatformCredentialRepository credentialRepository,
                                           TenantRepository tenantRepository,
                                           PlanRepository planRepository,
                                           SecretCipher secretCipher,
                                           ObjectMapper objectMapper) {
        this.providerRepository = providerRepository;
        this.modelRepository = modelRepository;
        this.credentialRepository = credentialRepository;
        this.tenantRepository = tenantRepository;
        this.planRepository = planRepository;
        this.secretCipher = secretCipher;
        this.objectMapper = objectMapper;
    }

    public List<PlatformProviderVO> listProviders() {
        return providerRepository.listAll().stream().map(this::toProviderVO).toList();
    }

    @Transactional
    public PlatformProviderVO createProvider(CreatePlatformProviderRequest request) {
        String code = request.providerCode().trim().toLowerCase(Locale.ROOT);
        if (providerRepository.findByCode(code).isPresent()) {
            throw new BusinessException(ErrorCode.CONFLICT, "服务商编码已存在");
        }
        PlatformProvider provider = new PlatformProvider();
        provider.setProviderCode(code);
        provider.setProviderName(request.providerName().trim());
        provider.setProviderType(request.providerType() == null || request.providerType().isBlank()
                ? "OPENAI_COMPATIBLE" : request.providerType());
        provider.setBaseUrl(trimToNull(request.baseUrl()));
        provider.setStatus(1);
        providerRepository.save(provider);
        return toProviderVO(provider);
    }

    @Transactional
    public PlatformProviderVO updateProvider(Long id, UpdatePlatformProviderRequest request) {
        PlatformProvider provider = requireProvider(id);
        if (request.providerName() != null && !request.providerName().isBlank()) {
            provider.setProviderName(request.providerName().trim());
        }
        if (request.providerType() != null && !request.providerType().isBlank()) {
            provider.setProviderType(request.providerType().trim());
        }
        if (request.baseUrl() != null) {
            provider.setBaseUrl(trimToNull(request.baseUrl()));
        }
        if (request.status() != null) {
            provider.setStatus(request.status() == 1 ? 1 : 0);
        }
        providerRepository.update(provider);
        return toProviderVO(provider);
    }

    @Transactional
    public void deleteProvider(Long id) {
        requireProvider(id);
        credentialRepository.deleteByProvider(id);
        modelRepository.deleteByProvider(id);
        providerRepository.delete(id);
    }

    public List<PlatformModelVO> listModelsAdmin(Long providerId) {
        Map<Long, PlatformProvider> providerMap = providerRepository.listAll().stream()
                .collect(Collectors.toMap(PlatformProvider::getId, Function.identity()));
        List<PlatformModel> models = providerId == null
                ? modelRepository.listAll()
                : modelRepository.listByProvider(providerId);
        return models.stream()
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
        Optional<PlatformModel> existing = modelRepository.findByProviderAndCodeIncludingDeleted(
                request.providerId(), request.modelCode().trim());
        if (existing.isPresent()) {
            PlatformModel current = existing.get();
            if (current.getDeleted() == null || current.getDeleted() == 0) {
                throw new BusinessException(ErrorCode.CONFLICT, "该服务商下已存在相同模型编码");
            }
            current.setModelName(model.getModelName());
            current.setDescription(model.getDescription());
            current.setContextWindow(model.getContextWindow());
            current.setMaxOutputTokens(model.getMaxOutputTokens());
            current.setSortOrder(model.getSortOrder());
            current.setStatus(1);
            modelRepository.restore(current.getId());
            modelRepository.update(current);
            return toModelVO(current, requireProvider(current.getProviderId()));
        }
        modelRepository.save(model);
        return toModelVO(model, requireProvider(model.getProviderId()));
    }

    @Transactional
    public PlatformModelVO updateModel(Long id, UpdatePlatformModelRequest request) {
        PlatformModel model = modelRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLATFORM_MODEL_NOT_FOUND, "平台模型不存在"));
        if (request.modelName() != null && !request.modelName().isBlank()) {
            model.setModelName(request.modelName().trim());
        }
        if (request.description() != null) {
            model.setDescription(trimToNull(request.description()));
        }
        if (request.contextWindow() != null) {
            model.setContextWindow(request.contextWindow());
        }
        if (request.maxOutputTokens() != null) {
            model.setMaxOutputTokens(request.maxOutputTokens());
        }
        if (request.sortOrder() != null) {
            model.setSortOrder(request.sortOrder());
        }
        if (request.status() != null) {
            model.setStatus(request.status() == 1 ? 1 : 0);
        }
        modelRepository.update(model);
        return toModelVO(model, requireProvider(model.getProviderId()));
    }

    @Transactional
    public void deleteModel(Long id) {
        modelRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLATFORM_MODEL_NOT_FOUND, "平台模型不存在"));
        modelRepository.delete(id);
    }

    public List<UpstreamModelVO> listUpstreamModels(Long providerId) {
        PlatformProvider provider = requireProvider(providerId);
        Set<String> imported = modelRepository.listByProvider(providerId).stream()
                .map(PlatformModel::getModelCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return fetchUpstreamModelSpecs(provider).stream()
                .map(spec -> new UpstreamModelVO(
                        spec.modelCode(),
                        spec.modelCode(),
                        imported.contains(spec.modelCode()),
                        positiveOrNull(spec.contextWindow()),
                        positiveOrNull(spec.maxOutputTokens())))
                .toList();
    }

    @Transactional
    public List<PlatformModelVO> importUpstreamModels(Long providerId, ImportPlatformModelsRequest request) {
        PlatformProvider provider = requireProvider(providerId);
        Map<String, UpstreamModelSpec> specs = indexSpecs(fetchUpstreamModelSpecs(provider));
        List<PlatformModelVO> created = new ArrayList<>();
        int sort = modelRepository.listByProvider(providerId).size();
        Set<String> seen = new LinkedHashSet<>();
        for (String rawCode : request.modelCodes()) {
            if (rawCode == null || rawCode.isBlank()) {
                continue;
            }
            String code = rawCode.trim();
            if (!seen.add(code.toLowerCase(Locale.ROOT))) {
                continue;
            }
            UpstreamModelSpec spec = specs.get(code.toLowerCase(Locale.ROOT));
            Optional<PlatformModel> existing = modelRepository.findByProviderAndCodeIncludingDeleted(providerId, code);
            if (existing.isPresent()) {
                PlatformModel current = existing.get();
                boolean restored = current.getDeleted() != null && current.getDeleted() != 0;
                if (restored) {
                    current.setStatus(1);
                    modelRepository.restore(current.getId());
                }
                applyUpstreamLimits(current, spec);
                if (restored) {
                    modelRepository.update(current);
                }
                modelRepository.updateLimits(current.getId(), current.getContextWindow(), current.getMaxOutputTokens());
                if (restored) {
                    created.add(toModelVO(current, provider));
                }
                continue;
            }
            PlatformModel model = new PlatformModel();
            model.setProviderId(providerId);
            model.setModelCode(code);
            model.setModelName(code);
            model.setModelType("CHAT");
            model.setSupportStreaming(true);
            model.setSortOrder(sort++);
            model.setStatus(1);
            applyUpstreamLimits(model, spec);
            modelRepository.save(model);
            created.add(toModelVO(model, provider));
        }
        syncLimitsFromUpstream(providerId, specs);
        return created;
    }

    @Transactional
    public int syncMissingModelLimits(Long providerId) {
        PlatformProvider provider = requireProvider(providerId);
        return syncLimitsFromUpstream(providerId, indexSpecs(fetchUpstreamModelSpecs(provider)));
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

    @Transactional
    public void deleteCredential(Long id) {
        credentialRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLATFORM_CREDENTIAL_MISSING, "平台密钥不存在"));
        credentialRepository.delete(id);
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
                .map(PlatformModelVO::id)
                .filter(this::isRunnable)
                .findFirst();
    }

    public Long requireRunnableModelId(Long preferredId) {
        if (isRunnable(preferredId)) {
            return preferredId;
        }
        return findFirstRunnableModelId().orElseThrow(() -> new BusinessException(
                ErrorCode.PLATFORM_CREDENTIAL_MISSING,
                "平台尚未配置可用模型密钥：请在管理端「平台模型池」为已上架模型绑定密钥"));
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

    private List<UpstreamModelSpec> fetchUpstreamModelSpecs(PlatformProvider provider) {
        if (provider.getBaseUrl() == null || provider.getBaseUrl().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请先填写服务商接口地址");
        }
        PlatformCredential credential = credentialRepository.findActiveByProvider(provider.getId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.PLATFORM_CREDENTIAL_MISSING,
                        "请先为「" + provider.getProviderName() + "」配置平台密钥，再从上游拉取模型"));
        String apiKey = secretCipher.decrypt(credential.getEncryptedApiKey());
        URI endpoint = SsrfGuard.validateHttpUrl(modelsEndpoint(provider.getBaseUrl()));
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> response = SsrfSafeHttpClient.send(httpClient, request, true, Duration.ofSeconds(20));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "上游返回 " + response.statusCode() + "，请检查接口地址与密钥");
            }
            return parseUpstreamModels(response.body());
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "拉取上游模型失败：" + ex.getMessage());
        }
    }

    private String modelsEndpoint(String baseUrl) {
        String trimmed = baseUrl.trim();
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.endsWith("/models")) {
            return trimmed;
        }
        return trimmed + "/models";
    }

    private List<UpstreamModelSpec> parseUpstreamModels(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.has("data") ? root.get("data") : root;
            if (!data.isArray()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "上游模型列表格式无法识别");
            }
            Map<String, UpstreamModelSpec> specs = new LinkedHashMap<>();
            for (JsonNode item : data) {
                String id = item.path("id").asText("");
                if (id.isBlank() && item.isTextual()) {
                    id = item.asText();
                }
                if (id.isBlank()) {
                    continue;
                }
                Integer context = firstPositiveInt(item,
                        "context_length", "context_window", "max_model_len", "max_context",
                        "contextLength", "contextWindow");
                if (context == null) {
                    context = firstPositiveInt(item.path("top_provider"), "context_length", "max_model_len");
                }
                if (context == null) {
                    context = firstPositiveInt(item.path("meta"), "context_length", "context_window");
                }
                Integer maxOutput = firstPositiveInt(item,
                        "max_completion_tokens", "max_output_tokens", "max_tokens",
                        "maxOutputTokens", "max_output");
                if (maxOutput == null) {
                    maxOutput = firstPositiveInt(item.path("top_provider"),
                            "max_completion_tokens", "max_output_tokens");
                }
                specs.putIfAbsent(id, new UpstreamModelSpec(id, context, maxOutput));
            }
            return specs.values().stream()
                    .sorted(Comparator.comparing(UpstreamModelSpec::modelCode, Comparator.naturalOrder()))
                    .toList();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "解析上游模型列表失败");
        }
    }

    private Map<String, UpstreamModelSpec> indexSpecs(List<UpstreamModelSpec> specs) {
        Map<String, UpstreamModelSpec> index = new LinkedHashMap<>();
        for (UpstreamModelSpec spec : specs) {
            index.put(spec.modelCode().toLowerCase(Locale.ROOT), spec);
        }
        return index;
    }

    private int syncLimitsFromUpstream(Long providerId, Map<String, UpstreamModelSpec> specs) {
        int updated = 0;
        for (PlatformModel model : modelRepository.listByProvider(providerId)) {
            if (model.getModelCode() == null || model.getId() == null) {
                continue;
            }
            Integer previousContext = model.getContextWindow();
            Integer previousMaxOutput = model.getMaxOutputTokens();
            applyUpstreamLimits(model, specs.get(model.getModelCode().toLowerCase(Locale.ROOT)));
            if (Objects.equals(previousContext, model.getContextWindow())
                    && Objects.equals(previousMaxOutput, model.getMaxOutputTokens())) {
                continue;
            }
            modelRepository.updateLimits(model.getId(), model.getContextWindow(), model.getMaxOutputTokens());
            updated++;
        }
        return updated;
    }

    private void applyUpstreamLimits(PlatformModel model, UpstreamModelSpec spec) {
        model.setContextWindow(spec == null ? null : positiveOrNull(spec.contextWindow()));
        model.setMaxOutputTokens(spec == null ? null : positiveOrNull(spec.maxOutputTokens()));
    }

    private Integer positiveOrNull(Integer value) {
        return value != null && value > 0 ? value : null;
    }

    private Integer firstPositiveInt(JsonNode node, String... fieldNames) {
        if (node == null || node.isMissingNode() || node.isNull() || !node.isObject()) {
            return null;
        }
        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value == null || value.isNull()) {
                continue;
            }
            if (value.isNumber()) {
                int number = value.asInt();
                if (number > 0) {
                    return number;
                }
            } else if (value.isTextual()) {
                try {
                    int number = Integer.parseInt(value.asText().trim());
                    if (number > 0) {
                        return number;
                    }
                } catch (NumberFormatException ignored) {
                    // ignore non-numeric metadata
                }
            }
        }
        return null;
    }

    private record UpstreamModelSpec(String modelCode, Integer contextWindow, Integer maxOutputTokens) {}

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
