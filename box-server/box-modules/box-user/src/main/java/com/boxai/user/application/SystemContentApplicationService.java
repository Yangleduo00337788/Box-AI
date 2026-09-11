package com.boxai.user.application;

import com.boxai.domain.config.SystemConfig;
import com.boxai.domain.config.SystemConfigRepository;
import com.boxai.user.api.SystemContentVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SystemContentApplicationService {

    private static final List<String> CONFIG_KEYS = List.of(
            "support.email",
            "about.slogan",
            "about.product_name",
            "about.positioning",
            "app.version",
            "legal.privacy",
            "legal.terms",
            "legal.updated_at");

    private final SystemConfigRepository systemConfigRepository;
    private final ObjectMapper objectMapper;

    public SystemContentApplicationService(SystemConfigRepository systemConfigRepository, ObjectMapper objectMapper) {
        this.systemConfigRepository = systemConfigRepository;
        this.objectMapper = objectMapper;
    }

    public SystemContentVO getContent() {
        Map<String, SystemConfig> configs = systemConfigRepository.findByKeys(CONFIG_KEYS).stream()
                .collect(Collectors.toMap(SystemConfig::getConfigKey, Function.identity(), (a, b) -> a));
        return new SystemContentVO(
                value(configs, "support.email"),
                new SystemContentVO.AboutContentVO(
                        value(configs, "about.slogan"),
                        value(configs, "about.product_name"),
                        value(configs, "about.positioning"),
                        value(configs, "app.version")),
                new SystemContentVO.LegalContentVO(
                        parseParagraphs(value(configs, "legal.privacy")),
                        parseParagraphs(value(configs, "legal.terms")),
                        value(configs, "legal.updated_at")));
    }

    private String value(Map<String, SystemConfig> configs, String key) {
        SystemConfig config = configs.get(key);
        return config == null ? null : config.getConfigValue();
    }

    private List<String> parseParagraphs(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of(raw);
        }
    }
}
