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
import com.boxai.runtime.workflow.core.WorkflowExecutionContext;
import com.boxai.runtime.workflow.core.WorkflowExecutionResult;
import com.boxai.runtime.workflow.core.WorkflowNodeTrace;
import com.boxai.runtime.workflow.engine.DefaultWorkflowExecutor;
import com.boxai.trace.application.ExecutionRecorder;
import com.boxai.workflow.api.WorkflowValidateVO;
import com.boxai.workflow.application.WorkflowApplicationService;
import com.boxai.workflow.application.WorkflowDefinitionValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WorkflowExecutionApplicationService {

    private final WorkflowVersionRepository workflowVersionRepository;
    private final WorkflowApplicationService workflowApplicationService;
    private final WorkflowDefinitionValidator workflowDefinitionValidator;
    private final DefaultWorkflowExecutor workflowExecutor;
    private final ExecutionRecorder executionRecorder;
    private final ObjectMapper objectMapper;

    public WorkflowExecutionApplicationService(WorkflowVersionRepository workflowVersionRepository,
                                                 WorkflowApplicationService workflowApplicationService,
                                                 WorkflowDefinitionValidator workflowDefinitionValidator,
                                                 DefaultWorkflowExecutor workflowExecutor,
                                                 ExecutionRecorder executionRecorder,
                                                 ObjectMapper objectMapper) {
        this.workflowVersionRepository = workflowVersionRepository;
        this.workflowApplicationService = workflowApplicationService;
        this.workflowDefinitionValidator = workflowDefinitionValidator;
        this.workflowExecutor = workflowExecutor;
        this.executionRecorder = executionRecorder;
        this.objectMapper = objectMapper;
    }

    public WorkflowExecutionResultVO debug(Long workflowId, WorkflowExecuteRequest request) {
        Workflow workflow = workflowApplicationService.requireWorkflow(workflowId);
        WorkflowVersion version = workflowApplicationService.requireDraft(workflow);
        return run(workflow, version, request, true);
    }

    public WorkflowExecutionResultVO execute(Long workflowId, WorkflowExecuteRequest request) {
        Workflow workflow = workflowApplicationService.requireWorkflow(workflowId);
        if (workflow.getPublishedVersionId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "工作流尚未发布，无法执行");
        }
        WorkflowVersion version = workflowVersionRepository.findById(workflow.getPublishedVersionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKFLOW_VERSION_NOT_FOUND, "发布版本不存在"));
        return run(workflow, version, request, false);
    }

    private WorkflowExecutionResultVO run(Workflow workflow,
                                          WorkflowVersion version,
                                          WorkflowExecuteRequest request,
                                          boolean debugMode) {
        WorkflowValidateVO validation = workflowDefinitionValidator.validate(version.getDefinitionJson());
        if (!validation.valid()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, String.join("；", validation.errors()));
        }

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
                new LinkedHashMap<>(inputs));

        WorkflowExecutionResult result = workflowExecutor.execute(version.getDefinitionJson(), context);
        String outputJson = toJson(Map.of(
                "outputs", result.outputs(),
                "nodeTraces", result.nodeTraces()));

        if ("SUCCEEDED".equals(result.status())) {
            executionRecorder.succeed(execution, outputJson, null);
        } else {
            executionRecorder.fail(execution, result.errorMessage());
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
}
