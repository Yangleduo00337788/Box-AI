package com.boxai.agent.application;

import com.boxai.agent.api.plugin.AdminPluginCatalogVO;
import com.boxai.agent.api.plugin.CreatePluginCatalogRequest;
import com.boxai.agent.api.plugin.UpdatePluginCatalogRequest;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.plugin.PluginCatalog;
import com.boxai.domain.plugin.PluginCatalogRepository;
import com.boxai.domain.plugin.WorkspacePluginInstallRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AdminPluginCatalogApplicationService {

    private static final Set<String> RESOURCE_CATEGORIES = Set.of("tools", "workflows", "knowledge", "mcp", "skills");

    private final PluginCatalogRepository pluginCatalogRepository;
    private final PluginCategoryApplicationService pluginCategoryApplicationService;
    private final WorkspacePluginInstallRepository workspacePluginInstallRepository;
    private final ObjectMapper objectMapper;

    public AdminPluginCatalogApplicationService(PluginCatalogRepository pluginCatalogRepository,
                                                PluginCategoryApplicationService pluginCategoryApplicationService,
                                                WorkspacePluginInstallRepository workspacePluginInstallRepository,
                                                ObjectMapper objectMapper) {
        this.pluginCatalogRepository = pluginCatalogRepository;
        this.pluginCategoryApplicationService = pluginCategoryApplicationService;
        this.workspacePluginInstallRepository = workspacePluginInstallRepository;
        this.objectMapper = objectMapper;
    }

    public List<AdminPluginCatalogVO> list(String category) {
        return pluginCatalogRepository.listAllForAdmin().stream()
                .filter(item -> category == null || category.isBlank() || category.equalsIgnoreCase(item.getCategory()))
                .map(this::toVO)
                .toList();
    }

    @Transactional
    public AdminPluginCatalogVO create(CreatePluginCatalogRequest request) {
        pluginCategoryApplicationService.requireActiveCategory(request.category());
        if (pluginCatalogRepository.findByCode(request.pluginCode()).isPresent()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "插件编码已存在");
        }
        String manifestJson = normalizeAndValidateManifest(request.category(), request.manifestJson());
        PluginCatalog plugin = new PluginCatalog();
        plugin.setPluginCode(request.pluginCode().trim());
        plugin.setCategory(request.category().trim());
        plugin.setTitle(request.title().trim());
        plugin.setDescription(request.description());
        plugin.setManifestJson(manifestJson);
        plugin.setStatus("LISTED");
        plugin.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        plugin.setInstallCount(0);
        pluginCatalogRepository.save(plugin);
        return toVO(plugin);
    }

    @Transactional
    public AdminPluginCatalogVO update(Long id, UpdatePluginCatalogRequest request) {
        PluginCatalog plugin = pluginCatalogRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLUGIN_NOT_FOUND, "插件不存在"));
        if (request.category() != null) {
            pluginCategoryApplicationService.requireActiveCategory(request.category());
            plugin.setCategory(request.category().trim());
        }
        if (request.title() != null) {
            plugin.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            plugin.setDescription(request.description());
        }
        if (request.manifestJson() != null) {
            plugin.setManifestJson(normalizeAndValidateManifest(plugin.getCategory(), request.manifestJson()));
        }
        if (request.sortOrder() != null) {
            plugin.setSortOrder(request.sortOrder());
        }
        if (request.status() != null) {
            plugin.setStatus(request.status());
        }
        pluginCatalogRepository.update(plugin);
        return toVO(plugin);
    }

    @Transactional
    public void delete(Long id) {
        PluginCatalog plugin = pluginCatalogRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLUGIN_NOT_FOUND, "插件不存在"));
        long installs = workspacePluginInstallRepository.countByPluginId(plugin.getId());
        if (installs > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "仍有工作空间安装了该插件，请先卸载后再删除");
        }
        pluginCatalogRepository.delete(id);
    }

    private String normalizeAndValidateManifest(String category, String manifestJson) {
        String normalized = PluginManifest.normalize(manifestJson, objectMapper);
        JsonNode node = PluginManifest.parse(normalized, objectMapper);
        String key = category == null ? "" : category.trim().toLowerCase(Locale.ROOT);
        if (!RESOURCE_CATEGORIES.contains(key)) {
            return normalized;
        }
        if ("tools".equals(key) && PluginManifest.text(node, "url", null) == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "工具插件需填写请求地址");
        }
        if ("mcp".equals(key) && PluginManifest.text(node, "endpointUrl", null) == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "MCP 插件需填写服务地址");
        }
        if ("skills".equals(key) && PluginManifest.text(node, "instructions", null) == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Skill 插件需填写技能说明");
        }
        return normalized;
    }

    private AdminPluginCatalogVO toVO(PluginCatalog plugin) {
        return new AdminPluginCatalogVO(
                plugin.getId(),
                plugin.getPluginCode(),
                plugin.getCategory(),
                plugin.getTitle(),
                plugin.getDescription(),
                plugin.getManifestJson(),
                plugin.getStatus(),
                plugin.getSortOrder(),
                plugin.getInstallCount());
    }
}
