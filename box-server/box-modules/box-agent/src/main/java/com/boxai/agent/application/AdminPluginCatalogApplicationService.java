package com.boxai.agent.application;

import com.boxai.agent.api.plugin.AdminPluginCatalogVO;
import com.boxai.agent.api.plugin.CreatePluginCatalogRequest;
import com.boxai.agent.api.plugin.UpdatePluginCatalogRequest;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.plugin.PluginCatalog;
import com.boxai.domain.plugin.PluginCatalogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminPluginCatalogApplicationService {

    private final PluginCatalogRepository pluginCatalogRepository;
    private final PluginCategoryApplicationService pluginCategoryApplicationService;

    public AdminPluginCatalogApplicationService(PluginCatalogRepository pluginCatalogRepository,
                                                PluginCategoryApplicationService pluginCategoryApplicationService) {
        this.pluginCatalogRepository = pluginCatalogRepository;
        this.pluginCategoryApplicationService = pluginCategoryApplicationService;
    }

    public List<AdminPluginCatalogVO> list() {
        return pluginCatalogRepository.listAllForAdmin().stream()
                .map(this::toVO)
                .toList();
    }

    @Transactional
    public AdminPluginCatalogVO create(CreatePluginCatalogRequest request) {
        pluginCategoryApplicationService.requireActiveCategory(request.category());
        if (pluginCatalogRepository.findByCode(request.pluginCode()).isPresent()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "插件编码已存在");
        }
        PluginCatalog plugin = new PluginCatalog();
        plugin.setPluginCode(request.pluginCode().trim());
        plugin.setCategory(request.category().trim());
        plugin.setTitle(request.title().trim());
        plugin.setDescription(request.description());
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
        if (request.sortOrder() != null) {
            plugin.setSortOrder(request.sortOrder());
        }
        if (request.status() != null) {
            plugin.setStatus(request.status());
        }
        pluginCatalogRepository.update(plugin);
        return toVO(plugin);
    }

    private AdminPluginCatalogVO toVO(PluginCatalog plugin) {
        return new AdminPluginCatalogVO(
                plugin.getId(),
                plugin.getPluginCode(),
                plugin.getCategory(),
                plugin.getTitle(),
                plugin.getDescription(),
                plugin.getStatus(),
                plugin.getSortOrder(),
                plugin.getInstallCount());
    }
}
