package com.boxai.model.application;

import com.boxai.ai.ModelRuntimeConfig;
import com.boxai.common.ai.PlatformModelClassifier;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.config.SystemConfig;
import com.boxai.domain.config.SystemConfigRepository;
import com.boxai.domain.platform.PlatformModel;
import com.boxai.domain.platform.PlatformModelRepository;
import com.boxai.domain.platform.PlatformProvider;
import com.boxai.domain.platform.PlatformProviderRepository;
import com.boxai.model.api.platform.PlatformOcrDefaultVO;
import com.boxai.model.api.platform.UpdatePlatformOcrDefaultRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class PlatformOcrSettingsApplicationService {

    public static final String CONFIG_KEY = "platform.ocr.default_model_id";

    private final SystemConfigRepository systemConfigRepository;
    private final PlatformModelApplicationService platformModelApplicationService;
    private final PlatformModelRepository platformModelRepository;
    private final PlatformProviderRepository platformProviderRepository;
    private final Long yamlFallbackModelId;

    public PlatformOcrSettingsApplicationService(SystemConfigRepository systemConfigRepository,
                                                 PlatformModelApplicationService platformModelApplicationService,
                                                 PlatformModelRepository platformModelRepository,
                                                 PlatformProviderRepository platformProviderRepository,
                                                 @Value("${box.ai.ocr.platform-model-id:}") Long yamlFallbackModelId) {
        this.systemConfigRepository = systemConfigRepository;
        this.platformModelApplicationService = platformModelApplicationService;
        this.platformModelRepository = platformModelRepository;
        this.platformProviderRepository = platformProviderRepository;
        this.yamlFallbackModelId = yamlFallbackModelId;
    }

    public PlatformOcrDefaultVO getSettings() {
        Long configuredId = readConfiguredModelId().orElse(null);
        Resolution resolution = resolveEffectiveResolution(configuredId);
        if (resolution.effectiveModelId() == null) {
            return new PlatformOcrDefaultVO(
                    configuredId,
                    null,
                    null,
                    null,
                    null,
                    false,
                    "NONE",
                    "未配置可用 OCR/视觉模型，请在平台模型池上架并绑定密钥");
        }
        return buildVo(configuredId, resolution.effectiveModelId(), resolution.mode(), resolution.hint());
    }

    @Transactional
    public PlatformOcrDefaultVO update(UpdatePlatformOcrDefaultRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请求不能为空");
        }
        Long modelId = request.platformModelId();
        if (modelId == null || modelId <= 0) {
            upsertConfig("");
            return getSettings();
        }
        PlatformModel model = platformModelRepository.findById(modelId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "模型不存在"));
        if (!PlatformModelClassifier.isOcrOrVisionModel(model.getModelCode(), model.getModelName(), model.getDescription())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择 OCR 或视觉多模态模型");
        }
        if (!platformModelApplicationService.isRunnable(modelId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "模型未上架或服务商密钥不可用");
        }
        upsertConfig(String.valueOf(modelId));
        return getSettings();
    }

    public Optional<Long> resolveRunnableModelId() {
        Long configuredId = readConfiguredModelId().orElse(null);
        return Optional.ofNullable(resolveEffectiveResolution(configuredId).effectiveModelId());
    }

    public Optional<ModelRuntimeConfig> resolveRuntimeConfig() {
        return resolveRunnableModelId()
                .map(id -> platformModelApplicationService.resolveForChat(id).runtimeConfig());
    }

    private Resolution resolveEffectiveResolution(Long configuredId) {
        if (configuredId != null && platformModelApplicationService.isRunnable(configuredId)) {
            return new Resolution(configuredId, "CONFIGURED", "使用管理端配置的默认 OCR 模型");
        }
        if (yamlFallbackModelId != null && yamlFallbackModelId > 0
                && platformModelApplicationService.isRunnable(yamlFallbackModelId)) {
            return new Resolution(yamlFallbackModelId, "YAML", "使用部署配置 box.ai.ocr.platform-model-id");
        }
        Optional<Long> autoOcr = platformModelApplicationService.findFirstRunnableOcrModelId();
        if (autoOcr.isPresent()) {
            return new Resolution(autoOcr.get(), "AUTO_OCR", "未配置默认模型，自动选择平台模型池中的 OCR 模型");
        }
        Optional<Long> autoVision = platformModelApplicationService.findFirstRunnableVisionModelId();
        if (autoVision.isPresent()) {
            return new Resolution(autoVision.get(), "AUTO_VISION", "未配置默认模型，自动选择平台模型池中的视觉模型");
        }
        return new Resolution(null, "NONE", "未配置可用 OCR/视觉模型");
    }

    private Optional<Long> readConfiguredModelId() {
        return systemConfigRepository.findByKey(CONFIG_KEY)
                .map(SystemConfig::getConfigValue)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .flatMap(this::parsePositiveLong);
    }

    private Optional<Long> parsePositiveLong(String value) {
        try {
            long parsed = Long.parseLong(value);
            return parsed > 0 ? Optional.of(parsed) : Optional.empty();
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private void upsertConfig(String value) {
        SystemConfig config = new SystemConfig();
        config.setConfigKey(CONFIG_KEY);
        config.setConfigValue(value == null ? "" : value);
        config.setDescription("平台默认 OCR/视觉模型（platform_model.id）");
        systemConfigRepository.upsert(config);
    }

    private PlatformOcrDefaultVO buildVo(Long configuredId, Long effectiveId, String mode, String hint) {
        PlatformModel model = platformModelRepository.findById(effectiveId).orElse(null);
        if (model == null) {
            return new PlatformOcrDefaultVO(
                    configuredId,
                    effectiveId,
                    null,
                    null,
                    null,
                    false,
                    mode,
                    hint + "（模型记录不存在）");
        }
        String providerName = platformProviderRepository.findById(model.getProviderId())
                .map(PlatformProvider::getProviderName)
                .orElse(null);
        boolean runnable = platformModelApplicationService.isRunnable(effectiveId);
        return new PlatformOcrDefaultVO(
                configuredId,
                effectiveId,
                model.getModelCode(),
                model.getModelName(),
                providerName,
                runnable,
                mode,
                hint);
    }

    private record Resolution(Long effectiveModelId, String mode, String hint) {
    }
}
