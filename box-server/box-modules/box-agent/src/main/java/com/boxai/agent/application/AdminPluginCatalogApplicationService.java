package com.boxai.agent.application;

import com.boxai.agent.api.plugin.AdminPluginCatalogVO;
import com.boxai.agent.api.plugin.CreatePluginCatalogRequest;
import com.boxai.agent.api.plugin.UpdatePluginCatalogRequest;
import com.boxai.agent.api.plugin.UpdatePluginReviewRequest;
import com.boxai.agent.api.market.UpdateMarketRolloutRequest;
import com.boxai.common.constant.MarketReviewStatuses;
import com.boxai.common.market.MarketRolloutSupport;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.plugin.PluginCatalog;
import com.boxai.domain.plugin.PluginCatalogRepository;
import com.boxai.domain.plugin.WorkspacePluginInstallRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class AdminPluginCatalogApplicationService {

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
        String manifestJson = PluginManifest.normalizeAndValidate(request.category(), request.manifestJson(), objectMapper);
        PluginCatalog plugin = new PluginCatalog();
        plugin.setPluginCode(request.pluginCode().trim());
        plugin.setCategory(request.category().trim());
        plugin.setTitle(request.title().trim());
        plugin.setDescription(request.description());
        plugin.setManifestJson(manifestJson);
        plugin.setStatus("LISTED");
        plugin.setReviewStatus(MarketReviewStatuses.PENDING_REVIEW);
        plugin.setSourceType("ADMIN");
        plugin.setVisibility("GLOBAL");
        plugin.setRolloutPercent(100);
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
            plugin.setManifestJson(PluginManifest.normalizeAndValidate(plugin.getCategory(), request.manifestJson(), objectMapper));
        }
        if (request.sortOrder() != null) {
            plugin.setSortOrder(request.sortOrder());
        }
        if (request.status() != null) {
            String status = request.status().trim().toUpperCase();
            if ("LISTED".equals(status) && !MarketReviewStatuses.isApproved(plugin.getReviewStatus())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "请先通过审核再上架");
            }
            plugin.setStatus(status);
        }
        pluginCatalogRepository.update(plugin);
        return toVO(plugin);
    }

    @Transactional
    public AdminPluginCatalogVO updateReview(Long id, UpdatePluginReviewRequest request) {
        PluginCatalog plugin = pluginCatalogRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLUGIN_NOT_FOUND, "插件不存在"));
        String reviewStatus = request.reviewStatus().trim().toUpperCase();
        if (!MarketReviewStatuses.ALL.contains(reviewStatus)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "审核状态无效");
        }
        plugin.setReviewStatus(reviewStatus);
        if (!MarketReviewStatuses.isApproved(reviewStatus) && "LISTED".equals(plugin.getStatus())) {
            plugin.setStatus("UNLISTED");
        }
        pluginCatalogRepository.update(plugin);
        return toVO(plugin);
    }

    @Transactional
    public AdminPluginCatalogVO updateRollout(Long id, UpdateMarketRolloutRequest request) {
        PluginCatalog plugin = pluginCatalogRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLUGIN_NOT_FOUND, "插件不存在"));
        String visibility = request.visibility().trim().toUpperCase();
        if (!Set.of("GLOBAL", "TENANT").contains(visibility)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "可见范围无效");
        }
        plugin.setVisibility(visibility);
        if ("TENANT".equals(visibility)) {
            var tenantIds = MarketRolloutSupport.parseTenantIdList(request.tenantIds());
            if (tenantIds.isEmpty()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "请填写至少一个租户 ID");
            }
            plugin.setTenantIdsJson(MarketRolloutSupport.serializeTenantIds(tenantIds));
        } else {
            plugin.setTenantIdsJson(null);
        }
        plugin.setRolloutPercent(request.rolloutPercent() == null ? 100 : request.rolloutPercent());
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

    private AdminPluginCatalogVO toVO(PluginCatalog plugin) {
        return new AdminPluginCatalogVO(
                plugin.getId(),
                plugin.getPluginCode(),
                plugin.getCategory(),
                plugin.getTitle(),
                plugin.getDescription(),
                plugin.getManifestJson(),
                plugin.getStatus(),
                plugin.getReviewStatus(),
                plugin.getSourceType(),
                plugin.getVisibility(),
                plugin.getTenantIdsJson(),
                plugin.getRolloutPercent(),
                plugin.getSortOrder(),
                plugin.getInstallCount());
    }
}
