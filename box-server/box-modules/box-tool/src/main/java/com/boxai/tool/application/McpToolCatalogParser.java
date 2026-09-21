package com.boxai.tool.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class McpToolCatalogParser {

    private static final Logger log = LoggerFactory.getLogger(McpToolCatalogParser.class);

    private final ObjectMapper objectMapper;

    public McpToolCatalogParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<McpCatalogTool> parse(String toolCatalogJson) {
        if (toolCatalogJson == null || toolCatalogJson.isBlank()) {
            return List.of();
        }
        try {
            List<Map<String, Object>> items = objectMapper.readValue(
                    toolCatalogJson, new TypeReference<List<Map<String, Object>>>() {});
            List<McpCatalogTool> tools = new ArrayList<>();
            for (Map<String, Object> item : items) {
                String name = stringValue(item.get("name"));
                if (name == null || name.isBlank()) {
                    continue;
                }
                String description = stringValue(item.get("description"));
                tools.add(new McpCatalogTool(name.trim(), description == null ? name.trim() : description.trim()));
            }
            return tools;
        } catch (Exception ex) {
            log.warn("Failed to parse MCP tool catalog JSON: {}", ex.getMessage());
            return List.of();
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    public record McpCatalogTool(String name, String description) {
    }
}
