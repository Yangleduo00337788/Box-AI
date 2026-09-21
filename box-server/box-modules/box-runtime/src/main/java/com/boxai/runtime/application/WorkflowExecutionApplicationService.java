package com.boxai.runtime.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.trace.Execution;
import com.boxai.domain.workflow.Workflow;
import com.boxai.domain.workflow.WorkflowVersion;
import com.boxai.domain.workflow.WorkflowVersionRepository;
import com.boxai.runtime.api.WorkflowExecuteRequest;
import com.boxai.runtime.api.WorkflowExecutionResultVO;
import com.boxai.runtime.api.WorkflowNodeTraceVO;
import com.boxai.runtime.api.WorkflowStreamEvent;
import com.boxai.runtime.workflow.core.WorkflowExecutionContext;
import com.boxai.runtime.workflow.core.WorkflowExecutionListener;
import com.boxai.runtime.workflow.core.WorkflowExecutionResult;
import com.boxai.runtime.workflow.core.WorkflowNodeTrace;
import com.boxai.runtime.workflow.core.WorkflowStreamCallback;
import com.boxai.runtime.workflow.engine.DefaultWorkflowExecutor;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.notification.NotificationPublisher;
import com.boxai.security.permission.WorkspacePermissionService;
import com.boxai.tenant.application.QuotaApplicationService;
import com.boxai.trace.application.ExecutionRecorder;
import com.boxai.workflow.api.WorkflowValidateVO;
import com.boxai.workflow.application.WorkflowApplicationService;
import com.boxai.workflow.application.WorkflowDefinitionValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class WorkflowExecutionApplicationService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutionApplicationService.class);

    private static final long STREAM_TIMEOUT_MS = 120_000L;
    private static final ExecutorService STREAM_EXECUTOR = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors()),
            runnable -> {
                Thread thread = new Thread(runnable, "box-workflow-stream");
                thread.setDaemon(true);
                return thread;
            });

    private final WorkflowVersionRepository workflowVersionRepository;
    private final WorkflowApplicationService workflowApplicationService;
    private final WorkflowDefinitionValidator workflowDefinitionValidator;
    private final DefaultWorkflowExecutor workflowExecutor;
    private final ExecutionRecorder executionRecorder;
    private final ObjectMapper objectMapper;
    private final WorkspacePermissionService workspacePermissionService;
    private final QuotaApplicationService quotaApplicationService;
    private final NotificationPublisher notificationPublisher;

    public WorkflowExecutionApplicationService(WorkflowVersionRepository workflowVersionRepository,
                                                 WorkflowApplicationService workflowApplicationService,
                                                 WorkflowDefinitionValidator workflowDefinitionValidator,
                                                 DefaultWorkflowExecutor workflowExecutor,
                                                 ExecutionRecorder executionRecorder,
                                                 ObjectMapper objectMapper,
                                                 WorkspacePermissionService workspacePermissionService,
                                                 QuotaApplicationService quotaApplicationService,
                                                 NotificationPublisher notificationPublisher) {
        this.workflowVersionRepository = workflowVersionRepository;
        this.workflowApplicationService = workflowApplicationService;
        this.workflowDefinitionValidator = workflowDefinitionValidator;
        this.workflowExecutor = workflowExecutor;
        this.executionRecorder = executionRecorder;
        this.objectMapper = objectMapper;
        this.workspacePermissionService = workspacePermissionService;
        this.quotaApplicationService = quotaApplicationService;
        this.notificationPublisher = notificationPublisher;
    }

    public WorkflowExecutionResultVO debug(Long workflowId, WorkflowExecuteRequest request) {
        workspacePermissionService.requirePermission(com.boxai.common.constant.PermissionCodes.WORKFLOW_EXECUTE);
        Workflow workflow = workflowApplicationService.requireWorkflow(workflowId);
        WorkflowVersion version = workflowApplicationService.requireDraft(workflow);
        return run(workflow, version, request, true, null, null);
    }

    public SseEmitter debugStream(Long workflowId, WorkflowExecuteRequest request, HttpServletResponse response) {
        workspacePermissionService.requirePermission(com.boxai.common.constant.PermissionCodes.WORKFLOW_EXECUTE);
        Workflow workflow = workflowApplicationService.requireWorkflow(workflowId);
        WorkflowVersion version = workflowApplicationService.requireDraft(workflow);
        configureSseResponse(response);
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        emitter.onTimeout(emitter::complete);
        WorkspaceContext workspaceContext = WorkspaceContext.get();
        SecurityContext securityContext = SecurityContextHolder.getContext();
        CompletableFuture.runAsync(() -> {
            if (workspaceContext != null) {
                WorkspaceContext.set(workspaceContext);
            }
            SecurityContextHolder.setContext(securityContext);
            try {
                WorkflowExecutionListener listener = new WorkflowExecutionListener() {
                    @Override
                    public void onNodeStart(String nodeId, String nodeType) {
                        sendStreamEvent(emitter, WorkflowStreamEvent.nodeStart(nodeId, nodeType));
                    }

                    @Override
                    public void onNodeComplete(WorkflowNodeTrace trace) {
                        sendStreamEvent(emitter, WorkflowStreamEvent.nodeEnd(
                                trace.nodeId(),
                                trace.nodeType(),
                                trace.status(),
                                trace.durationMs(),
                                trace.output(),
                                trace.errorMessage()));
                    }
                };
                WorkflowStreamCallback streamCallback = (nodeId, nodeType, chunk) -> {
                    if (listener != null) {
                        listener.onNodeDelta(nodeId, nodeType, chunk);
                    }
                    sendStreamEvent(emitter, WorkflowStreamEvent.nodeDelta(nodeId, nodeType, chunk));
                };
                WorkflowExecutionResultVO result = run(
                        workflow,
                        version,
                        request == null ? new WorkflowExecuteRequest(Map.of()) : request,
                        true,
                        listener,
                        streamCallback);
                sendStreamEvent(emitter, WorkflowStreamEvent.done(result));
                emitter.complete();
            } catch (Exception ex) {
                completeStreamWithError(emitter, ex);
            } finally {
                WorkspaceContext.clear();
                SecurityContextHolder.clearContext();
            }
        }, STREAM_EXECUTOR);
        return emitter;
    }

    public WorkflowExecutionResultVO execute(Long workflowId, WorkflowExecuteRequest request) {
        workspacePermissionService.requirePermission(com.boxai.common.constant.PermissionCodes.WORKFLOW_EXECUTE);
        Workflow workflow = workflowApplicationService.requireWorkflow(workflowId);
        if (workflow.getPublishedVersionId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "工作流尚未发布，无法执行");
        }
        WorkflowVersion version = workflowVersionRepository.findById(workflow.getPublishedVersionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKFLOW_VERSION_NOT_FOUND, "发布版本不存在"));
        return run(workflow, version, request, false, null, null);
    }

    public WorkflowExecutionResultVO executeWebhook(Long workflowId, WorkflowExecuteRequest request) {
        Workflow workflow = workflowApplicationService.requireWorkflow(workflowId);
        if (workflow.getPublishedVersionId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "工作流尚未发布，无法执行");
        }
        WorkflowVersion version = workflowVersionRepository.findById(workflow.getPublishedVersionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKFLOW_VERSION_NOT_FOUND, "发布版本不存在"));
        return run(workflow, version, request, false, null, null);
    }

    private WorkflowExecutionResultVO run(Workflow workflow,
                                          WorkflowVersion version,
                                          WorkflowExecuteRequest request,
                                          boolean debugMode,
                                          WorkflowExecutionListener listener,
                                          WorkflowStreamCallback streamCallback) {
        WorkflowValidateVO validation = workflowDefinitionValidator.validate(
                version.getDefinitionJson(), WorkspaceContext.require().workspaceId());
        if (!validation.valid()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, String.join("；", validation.errors()));
        }
        quotaApplicationService.assertAiQuotaAvailable(WorkspaceContext.require().workspaceId());

        Map<String, Object> inputs = request == null || request.inputs() == null
                ? Map.of()
                : request.inputs();
        String inputJson = toJson(Map.of("inputs", inputs, "debug", debugMode));
        Execution execution = executionRecorder.startWorkflowExecution(
                workflow.getId(), version.getId(), inputJson);

        WorkflowExecutionContext context = new WorkflowExecutionContext(
                workflow.getId(),
                version.getId(),
                execution.getId(),
                execution.getExecutionNo(),
                new LinkedHashMap<>(inputs),
                debugMode);
        if (listener != null) {
            context.setListener(listener);
        }
        if (streamCallback != null) {
            context.setStreamCallback(streamCallback);
        }

        WorkflowExecutionResult result = workflowExecutor.execute(version.getDefinitionJson(), context, listener);
        result.nodeTraces().forEach(trace -> executionRecorder.recordWorkflowNodeSpan(
                execution,
                trace.nodeId(),
                trace.nodeType(),
                trace.status(),
                trace.output(),
                trace.errorMessage()));
        String outputJson = toJson(Map.of(
                "outputs", result.outputs(),
                "nodeTraces", result.nodeTraces()));

        if ("SUCCEEDED".equals(result.status())) {
            executionRecorder.succeed(execution, outputJson, null);
        } else {
            executionRecorder.fail(execution, result.errorMessage());
            Long userId = WorkspaceContext.require().userId();
            notificationPublisher.publish(
                    userId,
                    workflow.getWorkspaceId(),
                    "工作流执行失败",
                    "工作流「" + workflow.getName() + "」执行失败："
                            + (result.errorMessage() == null ? "未知错误" : result.errorMessage()),
                    "WORKFLOW",
                    "/executions");
        }

        return toVO(workflow.getId(), version.getId(), result);
    }

    private WorkflowExecutionResultVO toVO(Long workflowId, Long versionId, WorkflowExecutionResult result) {
        List<WorkflowNodeTraceVO> traces = result.nodeTraces().stream()
                .map(this::toTraceVO)
                .toList();
        return new WorkflowExecutionResultVO(
                result.executionId(),
                result.executionNo(),
                result.status(),
                workflowId,
                versionId,
                result.outputs(),
                traces,
                result.errorMessage());
    }

    private WorkflowNodeTraceVO toTraceVO(WorkflowNodeTrace trace) {
        return new WorkflowNodeTraceVO(
                trace.nodeId(),
                trace.nodeType(),
                trace.status(),
                trace.durationMs(),
                trace.output(),
                trace.errorMessage());
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "执行结果序列化失败");
        }
    }

    private void configureSseResponse(HttpServletResponse response) {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
    }

    private void sendStreamEvent(SseEmitter emitter, WorkflowStreamEvent event) {
        try {
            synchronized (emitter) {
                emitter.send(SseEmitter.event().data(event));
            }
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "工作流调试流推送失败");
        }
    }

    private void completeStreamWithError(SseEmitter emitter, Exception error) {
        String message = error instanceof BusinessException businessException
                ? businessException.getMessage()
                : "工作流调试失败";
        try {
            sendStreamEvent(emitter, WorkflowStreamEvent.error(message));
        } catch (Exception ex) {
            log.debug("Failed to send workflow debug error event to client: {}", ex.getMessage());
        }
        emitter.completeWithError(error);
    }
}
