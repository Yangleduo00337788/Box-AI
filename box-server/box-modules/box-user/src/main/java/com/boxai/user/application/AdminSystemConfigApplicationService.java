package com.boxai.user.application;

import com.boxai.domain.config.SystemConfig;
import com.boxai.domain.config.SystemConfigRepository;
import com.boxai.user.api.AdminSystemConfigVO;
import com.boxai.user.api.UpdateSystemConfigRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminSystemConfigApplicationService {

    private final SystemConfigRepository systemConfigRepository;

    public AdminSystemConfigApplicationService(SystemConfigRepository systemConfigRepository) {
        this.systemConfigRepository = systemConfigRepository;
    }

    public List<AdminSystemConfigVO> list() {
        return systemConfigRepository.listAll().stream()
                .map(this::toVO)
                .toList();
    }

    @Transactional
    public AdminSystemConfigVO upsert(UpdateSystemConfigRequest request) {
        SystemConfig config = new SystemConfig();
        config.setConfigKey(request.configKey().trim());
        config.setConfigValue(request.configValue());
        config.setDescription(request.description());
        systemConfigRepository.upsert(config);
        return systemConfigRepository.findByKey(config.getConfigKey())
                .map(this::toVO)
                .orElse(toVO(config));
    }

    private AdminSystemConfigVO toVO(SystemConfig config) {
        return new AdminSystemConfigVO(
                config.getConfigKey(),
                config.getConfigValue(),
                config.getDescription());
    }
}
