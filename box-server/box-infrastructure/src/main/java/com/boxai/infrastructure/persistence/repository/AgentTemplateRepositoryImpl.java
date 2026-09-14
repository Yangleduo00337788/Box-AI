package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.agent.AgentTemplate;
import com.boxai.domain.agent.AgentTemplateRepository;
import com.boxai.infrastructure.persistence.entity.AgentTemplateDO;
import com.boxai.infrastructure.persistence.mapper.AgentTemplateMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class AgentTemplateRepositoryImpl implements AgentTemplateRepository {

    private final AgentTemplateMapper mapper;

    public AgentTemplateRepositoryImpl(AgentTemplateMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public AgentTemplate save(AgentTemplate template) {
        AgentTemplateDO row = toDo(template);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        row.setDeleted(0);
        if (row.getInstallCount() == null) {
            row.setInstallCount(0);
        }
        mapper.insert(row);
        template.setId(row.getId());
        template.setCreatedAt(row.getCreatedAt());
        template.setUpdatedAt(row.getUpdatedAt());
        return template;
    }

    @Override
    public void update(AgentTemplate template) {
        AgentTemplateDO row = toDo(template);
        row.setId(template.getId());
        row.setUpdatedAt(LocalDateTime.now());
        mapper.update(row);
        template.setUpdatedAt(row.getUpdatedAt());
    }

    @Override
    public Optional<AgentTemplate> findById(Long id) {
        return Optional.ofNullable(mapper.selectOneById(id)).map(this::toDomain);
    }

    @Override
    public List<AgentTemplate> listAll() {
        return mapper.selectListByQuery(QueryWrapper.create().orderBy("sort_order", false).orderBy("id", true))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<AgentTemplate> listListed() {
        return mapper.selectListByQuery(
                        QueryWrapper.create().eq("status", "LISTED").orderBy("sort_order", false).orderBy("id", true))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void incrementInstallCount(Long id) {
        UpdateChain.of(AgentTemplateDO.class)
                .setRaw("install_count", "install_count + 1")
                .set("updated_at", LocalDateTime.now())
                .where("id = ?", id)
                .update();
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    private AgentTemplate toDomain(AgentTemplateDO row) {
        AgentTemplate template = new AgentTemplate();
        template.setId(row.getId());
        template.setTemplateCode(row.getTemplateCode());
        template.setName(row.getName());
        template.setDescription(row.getDescription());
        template.setAvatarUrl(row.getAvatarUrl());
        template.setCategory(row.getCategory());
        template.setSystemPrompt(row.getSystemPrompt());
        template.setPlatformModelId(row.getPlatformModelId());
        template.setTemperature(row.getTemperature());
        template.setTopP(row.getTopP());
        template.setMaxTokens(row.getMaxTokens());
        template.setStreamEnabled(row.getStreamEnabled() != null && row.getStreamEnabled() == 1);
        template.setStatus(row.getStatus());
        template.setSortOrder(row.getSortOrder());
        template.setInstallCount(row.getInstallCount());
        template.setCreatedBy(row.getCreatedBy());
        template.setUpdatedBy(row.getUpdatedBy());
        template.setCreatedAt(row.getCreatedAt());
        template.setUpdatedAt(row.getUpdatedAt());
        return template;
    }

    private AgentTemplateDO toDo(AgentTemplate template) {
        AgentTemplateDO row = new AgentTemplateDO();
        row.setTemplateCode(template.getTemplateCode());
        row.setName(template.getName());
        row.setDescription(template.getDescription());
        row.setAvatarUrl(template.getAvatarUrl());
        row.setCategory(template.getCategory());
        row.setSystemPrompt(template.getSystemPrompt());
        row.setPlatformModelId(template.getPlatformModelId());
        row.setTemperature(template.getTemperature());
        row.setTopP(template.getTopP());
        row.setMaxTokens(template.getMaxTokens());
        row.setStreamEnabled(template.getStreamEnabled() == null || template.getStreamEnabled() ? 1 : 0);
        row.setStatus(template.getStatus() == null ? "DRAFT" : template.getStatus());
        row.setSortOrder(template.getSortOrder() == null ? 0 : template.getSortOrder());
        row.setInstallCount(template.getInstallCount());
        row.setCreatedBy(template.getCreatedBy());
        row.setUpdatedBy(template.getUpdatedBy());
        return row;
    }
}
