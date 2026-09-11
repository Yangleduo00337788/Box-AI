package com.boxai.tool.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.tool.Tool;
import com.boxai.domain.tool.ToolHttpConfig;
import com.boxai.domain.tool.ToolHttpConfigRepository;
import com.boxai.domain.tool.ToolRepository;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.tool.api.CreateToolRequest;
import com.boxai.tool.api.HttpToolConfigRequest;
import com.boxai.tool.api.HttpToolConfigVO;
import com.boxai.tool.api.ToolTestResultVO;
import com.boxai.tool.api.ToolVO;
import com.boxai.tool.api.UpdateToolRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ToolApplicationService {

    private final ToolRepository toolRepository;
    private final ToolHttpConfigRepository toolHttpConfigRepository;
    private final HttpToolExecutor httpToolExecutor;

    public ToolApplicationService(ToolRepository toolRepository,
                                  ToolHttpConfigRepository toolHttpConfigRepository,
                                  HttpToolExecutor httpToolExecutor) {
        this.toolRepository = toolRepository;
        this.toolHttpConfigRepository = toolHttpConfigRepository;
        this.httpToolExecutor = httpToolExecutor;
    }

    public List<ToolVO> list() {
        return toolRepository.listByWorkspace(workspaceId()).stream().map(this::toVO).toList();
    }

    public ToolVO detail(Long id) {
        return toVO(requireTool(id));
    }

    @Transactional
    public ToolVO create(CreateToolRequest request) {
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
        if ("HTTP".equals(request.type())) {
            saveHttpConfig(tool.getId(), request.httpConfig());
        }
        return toVO(tool);
    }

    @Transactional
    public ToolVO update(Long id, UpdateToolRequest request) {
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
        if ("HTTP".equals(request.type())) {
            toolHttpConfigRepository.findByToolId(tool.getId()).ifPresentOrElse(existing -> {
                applyHttpConfig(existing, request.httpConfig());
                toolHttpConfigRepository.update(existing);
            }, () -> saveHttpConfig(tool.getId(), request.httpConfig()));
        } else {
            toolHttpConfigRepository.deleteByToolId(tool.getId());
        }
        return toVO(tool);
    }

    @Transactional
    public void delete(Long id) {
        requireTool(id);
        toolHttpConfigRepository.deleteByToolId(id);
        toolRepository.delete(id);
    }

    public ToolTestResultVO test(Long id) {
        Tool tool = requireTool(id);
        if (!"HTTP".equals(tool.getType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前仅支持 HTTP 工具测试");
        }
        ToolHttpConfig config = toolHttpConfigRepository.findByToolId(tool.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "请先配置 HTTP 参数"));
        return httpToolExecutor.execute(config);
    }

    Tool requireTool(Long id) {
        Tool tool = toolRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOOL_NOT_FOUND, "工具不存在"));
        if (!workspaceId().equals(tool.getWorkspaceId())) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "无权访问该工具");
        }
        return tool;
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

    private void applyHttpConfig(ToolHttpConfig config, HttpToolConfigRequest request) {
        if (request == null) {
            return;
        }
        config.setMethod(request.method().trim().toUpperCase());
        config.setUrl(request.url().trim());
        config.setHeadersJson(trimToNull(request.headersJson()));
        config.setQueryParamsJson(trimToNull(request.queryParamsJson()));
        config.setBodyType(trimToNull(request.bodyType()));
        config.setBodyTemplate(trimToNull(request.bodyTemplate()));
        config.setTimeoutMs(request.timeoutMs() == null ? 10000 : request.timeoutMs());
        config.setAllowRedirect(Boolean.TRUE.equals(request.allowRedirect()));
    }

    private ToolVO toVO(Tool tool) {
        HttpToolConfigVO httpConfig = toolHttpConfigRepository.findByToolId(tool.getId())
                .map(this::toHttpVO)
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
