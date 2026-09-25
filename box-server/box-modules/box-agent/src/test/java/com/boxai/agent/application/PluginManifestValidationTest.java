package com.boxai.agent.application;

import com.boxai.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginManifestValidationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void skillAcceptsInstructionsOnly() {
        String json = "{\"instructions\":\"hello\"}";
        assertDoesNotThrow(() -> PluginManifest.normalizeAndValidate("skills", json, objectMapper));
    }

    @Test
    void knowledgeRequiresBundle() {
        assertThrows(BusinessException.class,
                () -> PluginManifest.normalizeAndValidate("knowledge", "{}", objectMapper));
    }

    @Test
    void mcpMergesMcpServersAlias() {
        String json = """
                {"mcpServers":{"local":{"url":"http://127.0.0.1:3100/mcp","transport":"HTTP"}}}
                """;
        String normalized = PluginManifest.normalizeAndValidate("mcp", json, objectMapper);
        assertTrue(normalized.contains("endpointUrl"));
    }
}
