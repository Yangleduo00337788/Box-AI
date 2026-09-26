package com.boxai.user.application;

import com.boxai.domain.config.SystemConfig;
import com.boxai.domain.config.SystemConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemContentApplicationServiceTest {

    @Mock
    private SystemConfigRepository systemConfigRepository;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private SystemContentApplicationService service;

    @Test
    void getContentRendersJsonLegalAsHtmlAndKeepsRawHtml() {
        when(systemConfigRepository.findByKeys(anyList())).thenReturn(List.of(
                config("about.product_name", "Box"),
                config("legal.privacy", "[\"第一条\",\"第二条\"]"),
                config("legal.terms", "<p>已是 HTML</p>"),
                config("legal.updated_at", "2026-01-01")));

        var vo = service.getContent();

        assertEquals("Box", vo.about().productName());
        assertEquals(List.of("第一条", "第二条"), vo.legal().privacy());
        assertEquals("<p>第一条</p><p>第二条</p>", vo.legal().privacyHtml());
        assertTrue(vo.legal().terms().isEmpty());
        assertEquals("<p>已是 HTML</p>", vo.legal().termsHtml());
        assertEquals("2026-01-01", vo.legal().updatedAt());
    }

    @Test
    void getContentWrapsPlainLegalText() {
        when(systemConfigRepository.findByKeys(anyList())).thenReturn(List.of(
                config("legal.privacy", "hello & world")));

        var vo = service.getContent();

        assertEquals(List.of("hello & world"), vo.legal().privacy());
        assertEquals("<p>hello &amp; world</p>", vo.legal().privacyHtml());
    }

    private static SystemConfig config(String key, String value) {
        SystemConfig config = new SystemConfig();
        config.setConfigKey(key);
        config.setConfigValue(value);
        return config;
    }
}
