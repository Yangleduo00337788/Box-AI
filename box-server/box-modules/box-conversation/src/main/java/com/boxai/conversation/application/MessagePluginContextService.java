package com.boxai.conversation.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.plugin.PluginCatalog;
import com.boxai.domain.plugin.PluginCatalogRepository;
import com.boxai.domain.plugin.WorkspacePluginInstall;
import com.boxai.domain.plugin.WorkspacePluginInstallRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
@Service
public class MessagePluginContextService {

    private static final int MAX_PLUGINS = 8;
    private static final Set<String> COMPOSER_CATEGORIES = Set.of("skills", "tools", "mcp");

    private final PluginCatalogRepository pluginCatalogRepository;
    private final WorkspacePluginInstallRepository workspacePluginInstallRepository;
    private final ObjectMapper objectMapper;

    public MessagePluginContextService(PluginCatalogRepository pluginCatalogRepository,
                                       WorkspacePluginInstallRepository workspacePluginInstallRepository,
                                       ObjectMapper objectMapper) {
        this.pluginCatalogRepository = pluginCatalogRepository;
        this.workspacePluginInstallRepository = workspacePluginInstallRepository;
        this.objectMapper = objectMapper;
    }

    public record PluginContextItem(
            Long id,
            String title,
            String category,
            String sourceType
    ) {
    }

    public record EnrichedMessage(String content, String metadataJson) {
    }

    public EnrichedMessage enrich(Long workspaceId, String message, List<Long> pluginIds) {
        String base = message == null ? "" : message.trim();
        if (pluginIds == null || pluginIds.isEmpty()) {
            return new EnrichedMessage(base, null);
        }
        if (pluginIds.size() > MAX_PLUGINS) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "单次消息最多启用 " + MAX_PLUGINS + " 个插件");
        }
        List<PluginContextItem> items = new ArrayList<>();
        for (Long pluginId : pluginIds) {
            if (pluginId == null) {
                continue;
            }
            WorkspacePluginInstall install = workspacePluginInstallRepository
                    .findByWorkspaceAndPlugin(workspaceId, pluginId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "插件未安装到当前工作空间"));
            PluginCatalog plugin = pluginCatalogRepository.findById(pluginId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.PLUGIN_NOT_FOUND, "插件不存在"));
            String category = plugin.getCategory() == null ? "" : plugin.getCategory().trim().toLowerCase(Locale.ROOT);
            if (!COMPOSER_CATEGORIES.contains(category)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "对话加号仅支持技能、工具与 MCP 插件，请在工作空间智能体中配置知识库与工作流");
            }
            items.add(new PluginContextItem(
                    plugin.getId(),
                    plugin.getTitle(),
                    plugin.getCategory(),
                    plugin.getSourceType() == null ? "ADMIN" : plugin.getSourceType()));
        }
        if (items.isEmpty()) {
            return new EnrichedMessage(base, null);
        }
        return new EnrichedMessage(base, pluginsMetadata(items));
    }

    private String pluginsMetadata(List<PluginContextItem> items) {
        try {
            List<Map<String, Object>> payload = items.stream()
                    .map(item -> {
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("id", item.id());
                        map.put("title", item.title());
                        map.put("category", item.category());
                        map.put("sourceType", item.sourceType());
                        return map;
                    })
                    .toList();
            return objectMapper.writeValueAsString(Map.of("plugins", payload));
        } catch (Exception ex) {
            return null;
        }
    }
}
