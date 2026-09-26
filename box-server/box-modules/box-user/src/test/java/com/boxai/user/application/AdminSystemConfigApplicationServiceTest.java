package com.boxai.user.application;

import com.boxai.domain.config.SystemConfig;
import com.boxai.domain.config.SystemConfigRepository;
import com.boxai.user.api.UpdateSystemConfigRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSystemConfigApplicationServiceTest {

    @Mock
    private SystemConfigRepository systemConfigRepository;

    @InjectMocks
    private AdminSystemConfigApplicationService service;

    @Test
    void listMapsConfigs() {
        SystemConfig config = new SystemConfig();
        config.setConfigKey("site.name");
        config.setConfigValue("Box");
        config.setDescription("产品名");
        when(systemConfigRepository.listAll()).thenReturn(List.of(config));

        var vos = service.list();

        assertEquals(1, vos.size());
        assertEquals("site.name", vos.get(0).configKey());
        assertEquals("Box", vos.get(0).configValue());
    }

    @Test
    void upsertTrimsKeyAndReloadsStoredValue() {
        SystemConfig stored = new SystemConfig();
        stored.setConfigKey("site.name");
        stored.setConfigValue("Box AI");
        stored.setDescription("产品名");
        when(systemConfigRepository.findByKey("site.name")).thenReturn(Optional.of(stored));

        var vo = service.upsert(new UpdateSystemConfigRequest("  site.name  ", "Box AI", "产品名"));

        ArgumentCaptor<SystemConfig> captor = ArgumentCaptor.forClass(SystemConfig.class);
        verify(systemConfigRepository).upsert(captor.capture());
        assertEquals("site.name", captor.getValue().getConfigKey());
        assertEquals("Box AI", vo.configValue());
    }
}
