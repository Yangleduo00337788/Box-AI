package com.boxai.runtime.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.common.security.WebhookSignature;
import com.boxai.domain.workflow.Workflow;
import com.boxai.domain.workflow.WorkflowRepository;
import com.boxai.runtime.api.WorkflowExecuteRequest;
import com.boxai.runtime.api.WorkflowExecutionResultVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowWebhookApplicationServiceTest {

    @Mock
    private WorkflowRepository workflowRepository;
    @Mock
    private WorkflowExecutionApplicationService workflowExecutionApplicationService;

    @Test
    void triggerRejectsUnknownToken() {
        var service = service();
        when(workflowRepository.findByWebhookToken("missing")).thenReturn(Optional.empty());
        BusinessException ex = assertThrows(BusinessException.class, () -> service.trigger("missing", "{}", "sig"));
        assertEquals(ErrorCode.NOT_FOUND, ex.getCode());
    }

    @Test
    void triggerRejectsUnpublishedWorkflow() {
        var service = service();
        Workflow workflow = publishedWorkflow();
        workflow.setStatus("DRAFT");
        when(workflowRepository.findByWebhookToken("tok")).thenReturn(Optional.of(workflow));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.trigger("tok", "{}", "sig"));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        verify(workflowExecutionApplicationService, never()).executeWebhook(anyLong(), any());
    }

    @Test
    void triggerRejectsInvalidSignature() {
        var service = service();
        when(workflowRepository.findByWebhookToken("tok")).thenReturn(Optional.of(publishedWorkflow()));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.trigger("tok", "{\"x\":1}", "sha256=deadbeef"));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getCode());
    }

    @Test
    void triggerExecutesPublishedWorkflowWithJsonPayload() {
        var service = service();
        Workflow workflow = publishedWorkflow();
        when(workflowRepository.findByWebhookToken("tok")).thenReturn(Optional.of(workflow));
        when(workflowExecutionApplicationService.executeWebhook(eq(5L), any()))
                .thenReturn(resultVo());
        String payload = "{\"foo\":1}";
        String signature = WebhookSignature.sign("secret", payload);

        var result = service.trigger("tok", payload, signature);

        assertEquals("SUCCEEDED", result.status());
        ArgumentCaptor<WorkflowExecuteRequest> captor = ArgumentCaptor.forClass(WorkflowExecuteRequest.class);
        verify(workflowExecutionApplicationService).executeWebhook(eq(5L), captor.capture());
        assertEquals(1, ((Map<?, ?>) captor.getValue().inputs().get("input")).get("foo"));
    }

    @Test
    void triggerWrapsNonJsonPayloadAsMessage() {
        var service = service();
        Workflow workflow = publishedWorkflow();
        workflow.setWebhookSecret("");
        when(workflowRepository.findByWebhookToken("tok")).thenReturn(Optional.of(workflow));
        when(workflowExecutionApplicationService.executeWebhook(eq(5L), any()))
                .thenReturn(resultVo());

        service.trigger("tok", "plain-text", "");

        ArgumentCaptor<WorkflowExecuteRequest> captor = ArgumentCaptor.forClass(WorkflowExecuteRequest.class);
        verify(workflowExecutionApplicationService).executeWebhook(eq(5L), captor.capture());
        assertEquals("plain-text", ((Map<?, ?>) captor.getValue().inputs().get("input")).get("message"));
    }

    private static WorkflowExecutionResultVO resultVo() {
        return new WorkflowExecutionResultVO(9L, "ex-9", "SUCCEEDED", 5L, 8L, Map.of(), List.of(), null);
    }

    private WorkflowWebhookApplicationService service() {
        return new WorkflowWebhookApplicationService(
                workflowRepository, workflowExecutionApplicationService, new ObjectMapper());
    }

    private static Workflow publishedWorkflow() {
        Workflow workflow = new Workflow();
        workflow.setId(5L);
        workflow.setWorkspaceId(7L);
        workflow.setCreatedBy(3L);
        workflow.setStatus("PUBLISHED");
        workflow.setPublishedVersionId(8L);
        workflow.setWebhookSecret("secret");
        return workflow;
    }
}
