package com.boxai.trace.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.trace.Execution;
import com.boxai.domain.trace.ExecutionRepository;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.trace.api.ExecutionVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExecutionApplicationService {

    private final ExecutionRepository executionRepository;

    public ExecutionApplicationService(ExecutionRepository executionRepository) {
        this.executionRepository = executionRepository;
    }

    public List<ExecutionVO> list(int limit) {
        int safeLimit = limit <= 0 ? 50 : Math.min(limit, 200);
        return executionRepository.listByWorkspace(workspaceId(), safeLimit).stream().map(this::toVO).toList();
    }

    public ExecutionVO detail(Long id) {
        return toVO(requireExecution(id));
    }

    Execution requireExecution(Long id) {
        Execution execution = executionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "执行记录不存在"));
        if (!workspaceId().equals(execution.getWorkspaceId())) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "无权访问该执行记录");
        }
        return execution;
    }

    private ExecutionVO toVO(Execution execution) {
        return new ExecutionVO(
                execution.getId(),
                execution.getExecutionNo(),
                execution.getExecutionType(),
                execution.getAgentId(),
                execution.getAgentVersionId(),
                execution.getConversationId(),
                execution.getUserId(),
                execution.getStatus(),
                execution.getInputJson(),
                execution.getOutputJson(),
                execution.getErrorMessage(),
                execution.getStartedAt(),
                execution.getFinishedAt(),
                execution.getDurationMs(),
                execution.getTotalTokens(),
                execution.getCreatedAt());
    }

    private Long workspaceId() {
        return WorkspaceContext.require().workspaceId();
    }
}
