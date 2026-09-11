package com.boxai.agent.application;

import com.boxai.agent.api.plugin.PluginCategoryVO;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.plugin.PluginCategory;
import com.boxai.domain.plugin.PluginCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PluginCategoryApplicationService {

    private final PluginCategoryRepository pluginCategoryRepository;

    public PluginCategoryApplicationService(PluginCategoryRepository pluginCategoryRepository) {
        this.pluginCategoryRepository = pluginCategoryRepository;
    }

    public List<PluginCategoryVO> listActive() {
        return pluginCategoryRepository.listActive().stream()
                .map(this::toVO)
                .toList();
    }

    public List<PluginCategoryVO> listAll() {
        return pluginCategoryRepository.listAll().stream()
                .map(this::toVO)
                .toList();
    }

    public void requireActiveCategory(String category) {
        if (category == null || category.isBlank()) {
            return;
        }
        if (!pluginCategoryRepository.existsActive(category.trim())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "插件分类无效");
        }
    }

    @Transactional
    public PluginCategoryVO create(String categoryCode, String label, String description, Integer sortOrder) {
        if (pluginCategoryRepository.findByCode(categoryCode).isPresent()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "分类编码已存在");
        }
        PluginCategory category = new PluginCategory();
        category.setCategoryCode(categoryCode);
        category.setLabel(label);
        category.setDescription(description);
        category.setSortOrder(sortOrder == null ? 0 : sortOrder);
        category.setStatus("ACTIVE");
        pluginCategoryRepository.save(category);
        return toVO(category);
    }

    @Transactional
    public PluginCategoryVO update(String categoryCode, String label, String description, Integer sortOrder, String status) {
        PluginCategory category = pluginCategoryRepository.findByCode(categoryCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "分类不存在"));
        if (label != null) {
            category.setLabel(label);
        }
        if (description != null) {
            category.setDescription(description);
        }
        if (sortOrder != null) {
            category.setSortOrder(sortOrder);
        }
        if (status != null) {
            category.setStatus(status);
        }
        pluginCategoryRepository.update(category);
        return toVO(category);
    }

    private PluginCategoryVO toVO(PluginCategory category) {
        return new PluginCategoryVO(
                category.getCategoryCode(),
                category.getLabel(),
                category.getDescription());
    }
}
