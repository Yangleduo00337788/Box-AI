package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.mcp.McpServer;
import com.boxai.domain.mcp.McpServerRepository;
import com.boxai.infrastructure.persistence.entity.McpServerDO;
import com.boxai.infrastructure.persistence.mapper.McpServerMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class McpServerRepositoryImpl implements McpServerRepository {

    private final McpServerMapper mapper;

    public McpServerRepositoryImpl(McpServerMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public McpServer save(McpServer server) {
        McpServerDO row = toDo(server);
        LocalDateTime now = LocalDateTime.now();
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        row.setDeleted(0);
        mapper.insert(row);
        server.setId(row.getId());
        server.setCreatedAt(row.getCreatedAt());
        server.setUpdatedAt(row.getUpdatedAt());
        return server;
    }

    @Override
    public void update(McpServer server) {
        McpServerDO row = toDo(server);
        row.setUpdatedAt(LocalDateTime.now());
        mapper.update(row);
        server.setUpdatedAt(row.getUpdatedAt());
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public Optional<McpServer> findById(Long id) {
        McpServerDO row = mapper.selectOneById(id);
        return row == null || row.getDeleted() != null && row.getDeleted() == 1 ? Optional.empty() : Optional.of(toDomain(row));
    }

    @Override
    public List<McpServer> listByWorkspace(Long workspaceId) {
        QueryWrapper query = QueryWrapper.create()
                .eq("workspace_id", workspaceId)
                .eq("deleted", 0)
                .orderBy("updated_at", false);
        return mapper.selectListByQuery(query).stream().map(this::toDomain).toList();
    }

    private McpServer toDomain(McpServerDO row) {
        McpServer server = new McpServer();
        server.setId(row.getId());
        server.setWorkspaceId(row.getWorkspaceId());
        server.setName(row.getName());
        server.setServerKey(row.getServerKey());
        server.setDescription(row.getDescription());
        server.setTransportType(row.getTransportType());
        server.setEndpointUrl(row.getEndpointUrl());
        server.setAuthType(row.getAuthType());
        server.setAuthConfigJson(row.getAuthConfig());
        server.setToolCatalogJson(row.getToolCatalogJson());
        server.setStatus(row.getStatus());
        server.setLastSyncAt(row.getLastSyncAt());
        server.setCreatedBy(row.getCreatedBy());
        server.setCreatedAt(row.getCreatedAt());
        server.setUpdatedAt(row.getUpdatedAt());
        return server;
    }

    private McpServerDO toDo(McpServer server) {
        McpServerDO row = new McpServerDO();
        row.setId(server.getId());
        row.setWorkspaceId(server.getWorkspaceId());
        row.setName(server.getName());
        row.setServerKey(server.getServerKey());
        row.setDescription(server.getDescription());
        row.setTransportType(server.getTransportType());
        row.setEndpointUrl(server.getEndpointUrl());
        row.setAuthType(server.getAuthType());
        row.setAuthConfig(server.getAuthConfigJson());
        row.setToolCatalogJson(server.getToolCatalogJson());
        row.setStatus(server.getStatus());
        row.setLastSyncAt(server.getLastSyncAt());
        row.setCreatedBy(server.getCreatedBy());
        return row;
    }
}
