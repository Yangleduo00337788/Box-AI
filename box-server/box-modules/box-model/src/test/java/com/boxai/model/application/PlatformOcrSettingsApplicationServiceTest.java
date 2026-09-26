package com.boxai.model.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.config.SystemConfig;
import com.boxai.domain.config.SystemConfigRepository;
import com.boxai.domain.platform.PlatformModel;
import com.boxai.domain.platform.PlatformModelRepository;
import com.boxai.domain.platform.PlatformProvider;
import com.boxai.domain.platform.PlatformProviderRepository;
import com.boxai.model.api.platform.UpdatePlatformOcrDefaultRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformOcrSettingsApplicationServiceTest {

    @Mock
    private SystemConfigRepository systemConfigRepository;
    @Mock
    private PlatformModelApplicationService platformModelApplicationService;
    @Mock
    private PlatformModelRepository platformModelRepository;
    @Mock
    private PlatformProviderRepository platformProviderRepository;

    @Test
    void getSettingsReportsNoneWhenNoRunnableModel() {
        when(systemConfigRepository.findByKey(PlatformOcrSettingsApplicationService.CONFIG_KEY))
                .thenReturn(Optional.empty());
        when(platformModelApplicationService.findFirstRunnableOcrModelId()).thenReturn(Optional.empty());
        when(platformModelApplicationService.findFirstRunnableVisionModelId()).thenReturn(Optional.empty());

        var vo = service(null).getSettings();

        assertNull(vo.platformModelId());
        assertEquals("NONE", vo.resolutionMode());
        assertFalse(vo.runnable());
    }

    @Test
    void updateRejectsNonVisionModel() {
        PlatformModel model = new PlatformModel();
        model.setId(5L);
        model.setModelCode("gpt-3.5-turbo");
        model.setModelName("GPT 3.5");
        when(platformModelRepository.findById(5L)).thenReturn(Optional.of(model));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service(null).update(new UpdatePlatformOcrDefaultRequest(5L)));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    }

    @Test
    void updatePersistsRunnableOcrModel() {
        PlatformModel model = new PlatformModel();
        model.setId(15L);
        model.setProviderId(2L);
        model.setModelCode("paddleocr");
        model.setModelName("OCR");
        when(platformModelRepository.findById(15L)).thenReturn(Optional.of(model));
        when(platformModelApplicationService.isRunnable(15L)).thenReturn(true);
        SystemConfig stored = new SystemConfig();
        stored.setConfigKey(PlatformOcrSettingsApplicationService.CONFIG_KEY);
        stored.setConfigValue("15");
        when(systemConfigRepository.findByKey(PlatformOcrSettingsApplicationService.CONFIG_KEY))
                .thenReturn(Optional.of(stored));
        PlatformProvider provider = new PlatformProvider();
        provider.setProviderName("Local");
        when(platformProviderRepository.findById(2L)).thenReturn(Optional.of(provider));

        var vo = service(null).update(new UpdatePlatformOcrDefaultRequest(15L));

        ArgumentCaptor<SystemConfig> captor = ArgumentCaptor.forClass(SystemConfig.class);
        verify(systemConfigRepository).upsert(captor.capture());
        assertEquals("15", captor.getValue().getConfigValue());
        assertEquals("CONFIGURED", vo.resolutionMode());
        assertEquals("paddleocr", vo.modelCode());
        assertEquals("Local", vo.providerName());
    }

    @Test
    void updateClearsConfigWhenModelIdMissing() {
        when(systemConfigRepository.findByKey(PlatformOcrSettingsApplicationService.CONFIG_KEY))
                .thenReturn(Optional.empty());
        when(platformModelApplicationService.findFirstRunnableOcrModelId()).thenReturn(Optional.empty());
        when(platformModelApplicationService.findFirstRunnableVisionModelId()).thenReturn(Optional.empty());

        service(null).update(new UpdatePlatformOcrDefaultRequest(null));

        ArgumentCaptor<SystemConfig> captor = ArgumentCaptor.forClass(SystemConfig.class);
        verify(systemConfigRepository).upsert(captor.capture());
        assertEquals("", captor.getValue().getConfigValue());
    }

    @Test
    void resolveFallsBackToYamlWhenConfiguredUnrunnable() {
        when(systemConfigRepository.findByKey(PlatformOcrSettingsApplicationService.CONFIG_KEY))
                .thenReturn(Optional.empty());
        when(platformModelApplicationService.isRunnable(99L)).thenReturn(true);

        assertEquals(99L, service(99L).resolveRunnableModelId().orElseThrow());
    }

    private PlatformOcrSettingsApplicationService service(Long yamlFallback) {
        return new PlatformOcrSettingsApplicationService(
                systemConfigRepository,
                platformModelApplicationService,
                platformModelRepository,
                platformProviderRepository,
                yamlFallback);
    }
}
