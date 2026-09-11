package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.tool.Tool;
import com.boxai.domain.tool.ToolRepository;
import com.boxai.infrastructure.persistence.entity.ToolDO;
import com.boxai.infrastructure.persistence.mapper.ToolMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ToolRepositoryImpl implements ToolRepository {

    private final ToolMapper mapper;

    public ToolRepositoryImpl(ToolMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Tool save(Tool tool) {
        ToolDO row = toDo(tool);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        row.setDeleted(0);
        mapper.insert(row);
        tool.setId(row.getId());
        tool.setCreatedAt(row.getCreatedAt());
        tool.setUpdatedAt(row.getUpdatedAt());
        return tool;
    }

    @Override
    public void update(Tool tool) {
        ToolDO row = toDo(tool);
        row.setId(tool.getId());
        row.setUpdatedAt(LocalDateTime.now());
        mapper.update(row);
        tool.setUpdatedAt(row.getUpdatedAt());
    }

    @Override
    public Optional<Tool> findById(Long id) {
        return Optional.ofNullable(mapper.selectOneById(id)).map(this::toDomain);
    }

    @Override
    public List<Tool> listByWorkspace(Long workspaceId) {
        return mapper.selectListByQuery(
                        QueryWrapper.create().eq("workspace_id", workspaceId).orderBy("updated_at", false))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    private Tool toDomain(ToolDO row) {
        Tool tool = new Tool();
        tool.setId(row.getId());
        tool.setWorkspaceId(row.getWorkspaceId());
        tool.setName(row.getName());
        tool.setToolKey(row.getToolKey());
        tool.setDescription(row.getDescription());
        tool.setType(row.getType());
        tool.setInputSchemaJson(row.getInputSchema());
        tool.setOutputSchemaJson(row.getOutputSchema());
        tool.setStatus(row.getStatus());
        tool.setCreatedBy(row.getCreatedBy());
        tool.setCreatedAt(row.getCreatedAt());
        tool.setUpdatedAt(row.getUpdatedAt());
        return tool;
    }

    private ToolDO toDo(Tool tool) {
        ToolDO row = new ToolDO();
        row.setWorkspaceId(tool.getWorkspaceId());
        row.setName(tool.getName());
        row.setToolKey(tool.getToolKey());
        row.setDescription(tool.getDescription());
        row.setType(tool.getType());
        row.setInputSchema(tool.getInputSchemaJson());
        row.setOutputSchema(tool.getOutputSchemaJson());
        row.setStatus(tool.getStatus());
        row.setCreatedBy(tool.getCreatedBy());
        return row;
    }
}
