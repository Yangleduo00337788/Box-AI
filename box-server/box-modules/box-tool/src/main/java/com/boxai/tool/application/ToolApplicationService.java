package com.boxai.tool.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.common.security.SsrfGuard;
import com.boxai.domain.agent.AgentToolRepository;
import com.boxai.domain.crypto.SecretCipher;
import com.boxai.domain.tool.Tool;
import com.boxai.domain.tool.ToolDatabaseConfig;
import com.boxai.domain.tool.ToolDatabaseConfigRepository;
import com.boxai.domain.tool.ToolFunctionConfig;
import com.boxai.domain.tool.ToolFunctionConfigRepository;
import com.boxai.domain.tool.ToolHttpConfig;
import com.boxai.domain.tool.ToolHttpConfigRepository;
import com.boxai.domain.tool.ToolRepository;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.permission.WorkspacePermissionService;
import com.boxai.tool.api.CreateToolRequest;
import com.boxai.tool.api.DatabaseToolConfigRequest;
import com.boxai.tool.api.DatabaseToolConfigVO;
import com.boxai.tool.api.FunctionToolConfigRequest;
import com.boxai.tool.api.FunctionToolConfigVO;
import com.boxai.tool.api.HttpToolConfigRequest;
import com.boxai.tool.api.HttpToolConfigVO;
import com.boxai.tool.api.ToolTestRequest;
import com.boxai.tool.api.ToolTestResultVO;
import com.boxai.tool.api.ToolVO;
import com.boxai.tool.api.UpdateToolRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class ToolApplicationService {

    private final ToolRepository toolRepository;
    private final ToolHttpConfigRepository toolHttpConfigRepository;
    private final ToolDatabaseConfigRepository toolDatabaseConfigRepository;
    private final ToolFunctionConfigRepository toolFunctionConfigRepository;
    private final ToolExecutionService toolExecutionService;
    private final AgentToolRepository agentToolRepository;
    private final WorkspacePermissionService workspacePermissionService;
    private final SecretCipher secretCipher;

    public ToolApplicationService(ToolRepository toolRepository,
                                  ToolHttpConfigRepository toolHttpConfigRepository,
                                  ToolDatabaseConfigRepository toolDatabaseConfigRepository,
                                  ToolFunctionConfigRepository toolFunctionConfigRepository,
                                  ToolExecutionService toolExecutionService,
                                  AgentToolRepository agentToolRepository,
                                  WorkspacePermissionService workspacePermissionService,
                                  SecretCipher secretCipher) {
        this.toolRepository = toolRepository;
        this.toolHttpConfigRepository = toolHttpConfigRepository;
        this.toolDatabaseConfigRepository = toolDatabaseConfigRepository;
        this.toolFunctionConfigRepository = toolFunctionConfigRepository;
        this.toolExecutionService = toolExecutionService;
        this.agentToolRepository = agentToolRepository;
        this.workspacePermissionService = workspacePermissionService;
        this.secretCipher = secretCipher;
    }

    public List<ToolVO> list() {
        workspacePermissionService.requirePermission("tool:execute");
        return toolRepository.listByWorkspace(workspaceId()).stream().map(this::toVO).toList();
    }

    public ToolVO detail(Long id) {
        workspacePermissionService.requirePermission("tool:execute");
        return toVO(requireTool(id));
    }

    @Transactional
    public ToolVO create(CreateToolRequest request) {
        workspacePermissionService.requirePermission("tool:create");
        Long userId = WorkspaceContext.require().userId();
        Tool tool = new Tool();
        tool.setWorkspaceId(workspaceId());
        tool.setName(request.name().trim());
        tool.setToolKey(request.toolKey().trim());
        tool.setDescription(trimToNull(request.description()));
        tool.setType(request.type());
        tool.setInputSchemaJson(trimToNull(request.inputSchemaJson()));
        tool.setOutputSchemaJson(trimToNull(request.outputSchemaJson()));
        tool.setStatus(1);
        tool.setCreatedBy(userId);
        toolRepository.save(tool);
        saveTypeConfig(tool.getId(), request.type(), request.httpConfig(), request.databaseConfig(), request.functionConfig());
        return toVO(tool);
    }

    @Transactional
    public ToolVO update(Long id, UpdateToolRequest request) {
        workspacePermissionService.requirePermission("tool:create");
        Tool tool = requireTool(id);
        tool.setName(request.name().trim());
        tool.setDescription(trimToNull(request.description()));
        tool.setType(request.type());
        tool.setInputSchemaJson(trimToNull(request.inputSchemaJson()));
        tool.setOutputSchemaJson(trimToNull(request.outputSchemaJson()));
        if (request.status() != null) {
            tool.setStatus(request.status());
        }
        toolRepository.update(tool);
        String preservedDbPassword = toolDatabaseConfigRepository.findByToolId(tool.getId())
                .map(ToolDatabaseConfig::getPasswordCiphertext)
                .orElse(null);
        clearTypeConfigs(tool.getId());
        saveTypeConfigOnUpdate(tool.getId(), request.type(), request.httpConfig(), request.databaseConfig(),
                request.functionConfig(), preservedDbPassword);
        return toVO(tool);
    }

    @Transactional
    public void delete(Long id) {
        workspacePermissionService.requirePermission("tool:create");
        Tool tool = requireTool(id);
        int bindingCount = agentToolRepository.countByToolId(tool.getId());
        if (bindingCount > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "该工具已被 " + bindingCount + " 个智能体绑定，请先解除绑定后再删除");
        }
        clearTypeConfigs(id);
        toolRepository.delete(id);
    }

    public ToolTestResultVO test(Long id, ToolTestRequest request) {
        workspacePermissionService.requirePermission("tool:execute");
        Tool tool = requireTool(id);
        String sql = request == null ? null : request.sql();
        Map<String, Object> arguments = request == null ? Map.of() : request.arguments();
        return toolExecutionService.test(tool, sql, arguments);
    }

    Tool requireTool(Long id) {
        Tool tool = toolRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOOL_NOT_FOUND, "工具不存在"));
        if (!workspaceId().equals(tool.getWorkspaceId())) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "无权访问该工具");
        }
        return tool;
    }

    private void saveTypeConfig(Long toolId,
                                String type,
                                HttpToolConfigRequest httpConfig,
                                DatabaseToolConfigRequest databaseConfig,
                                FunctionToolConfigRequest functionConfig) {
        saveTypeConfigOnUpdate(toolId, type, httpConfig, databaseConfig, functionConfig, null);
    }

    private void saveTypeConfigOnUpdate(Long toolId,
                                        String type,
                                        HttpToolConfigRequest httpConfig,
                                        DatabaseToolConfigRequest databaseConfig,
                                        FunctionToolConfigRequest functionConfig,
                                        String preservedDbPassword) {
        switch (type) {
            case "HTTP" -> saveHttpConfig(toolId, httpConfig);
            case "DATABASE" -> saveDatabaseConfig(toolId, databaseConfig, preservedDbPassword);
            case "FUNCTION", "CODE" -> saveFunctionConfig(toolId, functionConfig);
            case "MCP" -> throw new BusinessException(ErrorCode.BAD_REQUEST, "MCP 请通过 MCP Server 管理，不要创建 MCP 类型工具");
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的工具类型: " + type);
        }
    }

    private void clearTypeConfigs(Long toolId) {
        toolHttpConfigRepository.deleteByToolId(toolId);
        toolDatabaseConfigRepository.deleteByToolId(toolId);
        toolFunctionConfigRepository.deleteByToolId(toolId);
    }

    private void saveHttpConfig(Long toolId, HttpToolConfigRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "HTTP 工具需要配置请求参数");
        }
        ToolHttpConfig config = new ToolHttpConfig();
        config.setToolId(toolId);
        applyHttpConfig(config, request);
        toolHttpConfigRepository.save(config);
    }

    private void saveDatabaseConfig(Long toolId, DatabaseToolConfigRequest request, String preservedDbPassword) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "数据库工具需要配置连接参数");
        }
        ToolDatabaseConfig config = new ToolDatabaseConfig();
        config.setToolId(toolId);
        boolean hasPassword = request.password() != null && !request.password().isBlank();
        if (hasPassword) {
            applyDatabaseConfig(config, request, true);
        } else if (preservedDbPassword != null && !preservedDbPassword.isBlank()) {
            applyDatabaseConfig(config, request, false);
            config.setPasswordCiphertext(preservedDbPassword);
        } else {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "数据库工具需要配置密码");
        }
        toolDatabaseConfigRepository.save(config);
    }

    private void saveFunctionConfig(Long toolId, FunctionToolConfigRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "脚本工具需要配置函数代码");
        }
        ToolFunctionConfig config = new ToolFunctionConfig();
        config.setToolId(toolId);
        applyFunctionConfig(config, request);
        toolFunctionConfigRepository.save(config);
    }

    private void applyHttpConfig(ToolHttpConfig config, HttpToolConfigRequest request) {
        if (request == null) {
            return;
        }
        config.setMethod(request.method().trim().toUpperCase());
        config.setUrl(SsrfGuard.validateHttpUrl(request.url().trim()).toString());
        config.setHeadersJson(trimToNull(request.headersJson()));
        config.setQueryParamsJson(trimToNull(request.queryParamsJson()));
        config.setBodyType(trimToNull(request.bodyType()));
        config.setBodyTemplate(trimToNull(request.bodyTemplate()));
        config.setTimeoutMs(request.timeoutMs() == null ? 10000 : request.timeoutMs());
        config.setAllowRedirect(Boolean.TRUE.equals(request.allowRedirect()));
    }

    private void applyDatabaseConfig(ToolDatabaseConfig config, DatabaseToolConfigRequest request, boolean encryptPassword) {
        config.setDatabaseType(request.databaseType().trim().toUpperCase());
        config.setHost(request.host().trim());
        config.setPort(request.port() == null ? 3306 : request.port());
        config.setDatabaseName(request.databaseName().trim());
        config.setUsername(request.username().trim());
        if (encryptPassword || request.password() != null && !request.password().isBlank()) {
            config.setPasswordCiphertext(secretCipher.encrypt(request.password()));
        }
        config.setAllowedOperationsJson(trimToNull(request.allowedOperationsJson()));
        config.setMaxRows(request.maxRows() == null ? 100 : request.maxRows());
        config.setTimeoutMs(request.timeoutMs() == null ? 10000 : request.timeoutMs());
    }

    private void applyFunctionConfig(ToolFunctionConfig config, FunctionToolConfigRequest request) {
        config.setFunctionName(request.functionName().trim());
        config.setFunctionCode(request.functionCode());
        config.setRuntime(request.runtime().trim().toUpperCase());
        config.setTimeoutMs(request.timeoutMs() == null ? 5000 : request.timeoutMs());
        config.setMemoryLimitMb(request.memoryLimitMb() == null ? 128 : request.memoryLimitMb());
    }

    private ToolVO toVO(Tool tool) {
        HttpToolConfigVO httpConfig = toolHttpConfigRepository.findByToolId(tool.getId())
                .map(this::toHttpVO)
                .orElse(null);
        DatabaseToolConfigVO databaseConfig = toolDatabaseConfigRepository.findByToolId(tool.getId())
                .map(this::toDatabaseVO)
                .orElse(null);
        FunctionToolConfigVO functionConfig = toolFunctionConfigRepository.findByToolId(tool.getId())
                .map(this::toFunctionVO)
                .orElse(null);
        return new ToolVO(
                tool.getId(),
                tool.getName(),
                tool.getToolKey(),
                tool.getDescription(),
                tool.getType(),
                tool.getInputSchemaJson(),
                tool.getOutputSchemaJson(),
                tool.getStatus(),
                httpConfig,
                databaseConfig,
                functionConfig,
                tool.getCreatedAt(),
                tool.getUpdatedAt());
    }

    private HttpToolConfigVO toHttpVO(ToolHttpConfig config) {
        return new HttpToolConfigVO(
                config.getMethod(),
                config.getUrl(),
                config.getHeadersJson(),
                config.getQueryParamsJson(),
                config.getBodyType(),
                config.getBodyTemplate(),
                config.getTimeoutMs(),
                config.getAllowRedirect());
    }

    private DatabaseToolConfigVO toDatabaseVO(ToolDatabaseConfig config) {
        return new DatabaseToolConfigVO(
                config.getDatabaseType(),
                config.getHost(),
                config.getPort(),
                config.getDatabaseName(),
                config.getUsername(),
                config.getAllowedOperationsJson(),
                config.getMaxRows(),
                config.getTimeoutMs());
    }

    private FunctionToolConfigVO toFunctionVO(ToolFunctionConfig config) {
        return new FunctionToolConfigVO(
                config.getFunctionName(),
                config.getFunctionCode(),
                config.getRuntime(),
                config.getTimeoutMs(),
                config.getMemoryLimitMb());
    }

    private Long workspaceId() {
        return WorkspaceContext.require().workspaceId();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
