package com.boxai.trace.application;

import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.trace.Execution;
import com.boxai.domain.trace.ExecutionRepository;
import com.boxai.domain.trace.Trace;
import com.boxai.domain.trace.TraceRepository;
import com.boxai.domain.trace.TraceSpan;
import com.boxai.domain.trace.TraceSpanRepository;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.permission.WorkspacePermissionService;
import com.boxai.trace.api.ExecutionVO;
import com.boxai.trace.api.TraceDetailVO;
import com.boxai.trace.api.TraceSpanVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExecutionApplicationService {

    private final ExecutionRepository executionRepository;
    private final TraceRepository traceRepository;
    private final TraceSpanRepository traceSpanRepository;
    private final WorkspacePermissionService workspacePermissionService;

    public ExecutionApplicationService(ExecutionRepository executionRepository,
                                         TraceRepository traceRepository,
                                         TraceSpanRepository traceSpanRepository,
                                         WorkspacePermissionService workspacePermissionService) {
        this.executionRepository = executionRepository;
        this.traceRepository = traceRepository;
        this.traceSpanRepository = traceSpanRepository;
        this.workspacePermissionService = workspacePermissionService;
    }

    public List<ExecutionVO> list(int limit) {
        return list(limit, null, null, null, null);
    }

    public List<ExecutionVO> list(int limit,
                                  String executionType,
                                  String status,
                                  Long agentId,
                                  Long conversationId) {
        workspacePermissionService.requirePermission(PermissionCodes.AGENT_READ);
        int safeLimit = limit <= 0 ? 50 : Math.min(limit, 200);
        return executionRepository.listByWorkspace(
                        workspaceId(),
                        executionType,
                        status,
                        agentId,
                        conversationId,
                        safeLimit)
                .stream()
                .map(this::toVO)
                .toList();
    }

    public ExecutionVO detail(Long id) {
        workspacePermissionService.requirePermission(PermissionCodes.AGENT_READ);
        return toVO(requireExecution(id));
    }

    public ExecutionVO latestByConversation(Long conversationId) {
        workspacePermissionService.requirePermission(PermissionCodes.AGENT_READ);
        return executionRepository.listByConversation(conversationId, 1).stream()
                .findFirst()
                .map(this::toVO)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "暂无执行记录"));
    }

    public TraceDetailVO trace(Long id) {
        workspacePermissionService.requirePermission(PermissionCodes.AGENT_READ);
        Execution execution = requireExecution(id);
        Trace trace = traceRepository.findByExecutionId(execution.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Trace 不存在"));
        List<TraceSpanVO> spans = traceSpanRepository.listByTraceId(trace.getTraceId()).stream()
                .map(this::toSpanVO)
                .toList();
        return new TraceDetailVO(trace.getTraceId(), execution.getId(), trace.getStatus(), spans);
    }

    Execution requireExecution(Long id) {
        Execution execution = executionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "执行记录不存在"));
        if (!workspaceId().equals(execution.getWorkspaceId())) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "无权访问该执行记录");
        }
        return execution;
    }

    private TraceSpanVO toSpanVO(TraceSpan span) {
        return new TraceSpanVO(
                span.getSpanId(),
                span.getParentSpanId(),
                span.getSpanType(),
                span.getName(),
                span.getStatus(),
                span.getInputJson(),
                span.getOutputJson(),
                span.getDurationMs(),
                span.getErrorMessage(),
                span.getStartTime(),
                span.getEndTime());
    }

    private ExecutionVO toVO(Execution execution) {
        return new ExecutionVO(
                execution.getId(),
                execution.getExecutionNo(),
                execution.getRequestId(),
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
