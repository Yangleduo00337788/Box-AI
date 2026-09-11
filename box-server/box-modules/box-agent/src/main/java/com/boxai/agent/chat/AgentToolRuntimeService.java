package com.boxai.agent.chat;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.agent.AgentTool;
import com.boxai.domain.agent.AgentToolRepository;
import com.boxai.domain.tool.Tool;
import com.boxai.domain.tool.ToolHttpConfig;
import com.boxai.domain.tool.ToolHttpConfigRepository;
import com.boxai.domain.tool.ToolRepository;
import com.boxai.tool.api.ToolTestResultVO;
import com.boxai.tool.application.HttpToolExecutor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AgentToolRuntimeService {

    private final AgentToolRepository agentToolRepository;
    private final ToolRepository toolRepository;
    private final ToolHttpConfigRepository toolHttpConfigRepository;
    private final HttpToolExecutor httpToolExecutor;

    public AgentToolRuntimeService(AgentToolRepository agentToolRepository,
                                   ToolRepository toolRepository,
                                   ToolHttpConfigRepository toolHttpConfigRepository,
                                   HttpToolExecutor httpToolExecutor) {
        this.agentToolRepository = agentToolRepository;
        this.toolRepository = toolRepository;
        this.toolHttpConfigRepository = toolHttpConfigRepository;
        this.httpToolExecutor = httpToolExecutor;
    }

    public List<ResolvedAgentTool> resolveTools(Long versionId) {
        List<AgentTool> bindings = agentToolRepository.listByVersionId(versionId);
        List<ResolvedAgentTool> tools = new ArrayList<>();
        for (AgentTool binding : bindings) {
            if (Boolean.FALSE.equals(binding.getEnabled())) {
                continue;
            }
            Tool tool = toolRepository.findById(binding.getToolId()).orElse(null);
            if (tool == null || tool.getStatus() == null || tool.getStatus() != 1) {
                continue;
            }
            tools.add(new ResolvedAgentTool(
                    tool.getId(),
                    tool.getToolKey(),
                    tool.getName(),
                    tool.getDescription(),
                    tool.getType()));
        }
        return tools;
    }

    public String execute(Long toolId) {
        Tool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOOL_NOT_FOUND, "工具不存在"));
        if (!"HTTP".equals(tool.getType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前仅支持 HTTP 工具调用");
        }
        ToolHttpConfig config = toolHttpConfigRepository.findByToolId(tool.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "HTTP 工具未配置"));
        ToolTestResultVO result = httpToolExecutor.execute(config);
        return result.body() == null ? "" : result.body();
    }

    public String executeByKey(List<ResolvedAgentTool> tools, String toolKey) {
        return tools.stream()
                .filter(item -> item.toolKey().equals(toolKey))
                .findFirst()
                .map(item -> execute(item.toolId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.TOOL_NOT_FOUND, "未找到工具: " + toolKey));
    }
}
