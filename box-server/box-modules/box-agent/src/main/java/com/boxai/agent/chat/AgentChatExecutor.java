package com.boxai.agent.chat;

import com.boxai.agent.api.ChatStreamEvent;
import com.boxai.ai.ChatModelGateway;
import com.boxai.ai.ChatStreamHandler;
import com.boxai.ai.ToolCall;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.common.exception.ToolConfirmationRequiredException;
import com.boxai.domain.model.ModelCredentialRepository;
import com.boxai.model.application.PlatformModelApplicationService;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.ratelimit.RateLimitService;
import com.boxai.domain.trace.Execution;
import com.boxai.tenant.application.QuotaApplicationService;
import com.boxai.trace.application.ExecutionRecorder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.boxai.security.context.WorkspaceContext;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Service
public class AgentChatExecutor {

    private static final long STREAM_TIMEOUT_MS = 120_000L;
    private static final ExecutorService STREAM_EXECUTOR = Executors.newFixedThreadPool(
            Math.max(4, Runtime.getRuntime().availableProcessors()),
            runnable -> {
                Thread thread = new Thread(runnable, "box-chat-stream");
                thread.setDaemon(true);
                return thread;
            });

    private final ChatModelGateway chatModelGateway;
    private final ModelCredentialRepository credentialRepository;
    private final QuotaApplicationService quotaApplicationService;
    private final PlatformModelApplicationService platformModelApplicationService;
    private final AgentToolRuntimeService agentToolRuntimeService;
    private final ExecutionRecorder executionRecorder;
    private final ObjectMapper objectMapper;
    private final RateLimitService rateLimitService;
    private final ToolConfirmationService toolConfirmationService;

    public AgentChatExecutor(ChatModelGateway chatModelGateway,
                             ModelCredentialRepository credentialRepository,
                             QuotaApplicationService quotaApplicationService,
                             PlatformModelApplicationService platformModelApplicationService,
                             AgentToolRuntimeService agentToolRuntimeService,
                             ExecutionRecorder executionRecorder,
                             ObjectMapper objectMapper,
                             RateLimitService rateLimitService,
                             ToolConfirmationService toolConfirmationService) {
        this.chatModelGateway = chatModelGateway;
        this.credentialRepository = credentialRepository;
        this.quotaApplicationService = quotaApplicationService;
        this.platformModelApplicationService = platformModelApplicationService;
        this.agentToolRuntimeService = agentToolRuntimeService;
        this.executionRecorder = executionRecorder;
        this.objectMapper = objectMapper;
        this.rateLimitService = rateLimitService;
        this.toolConfirmationService = toolConfirmationService;
    }

    public void assertQuotaAvailable() {
        quotaApplicationService.assertAiQuotaAvailable(WorkspaceContext.require().workspaceId());
    }

    public void assertChatRateLimit() {
        Long workspaceId = WorkspaceContext.require().workspaceId();
        Long userId = WorkspaceContext.require().userId();
        rateLimitService.assertAllowed(
                "chat",
                workspaceId + ":" + userId,
                60,
                Duration.ofMinutes(1));
    }

    public void assertPublishedChatRateLimit(Long agentId) {
        String ip = com.boxai.security.audit.HttpRequestContext.clientIp();
        rateLimitService.assertAllowed(
                "published-chat",
                agentId + ":" + (ip == null ? "unknown" : ip),
                60,
                Duration.ofMinutes(1));
    }

    public String chat(PreparedAgentChat prepared) {
        return chat(prepared, null);
    }

    public String chat(PreparedAgentChat prepared, Execution execution) {
        Long workspaceId = WorkspaceContext.require().workspaceId();
        assertChatRateLimit();
        quotaApplicationService.assertAiQuotaAvailable(workspaceId);
        try {
            String content;
            if (prepared.tools() != null && !prepared.tools().isEmpty()) {
                List<ResolvedAgentTool> resolvedTools = agentToolRuntimeService.resolveTools(prepared.agentVersionId());
                content = chatModelGateway.chatWithTools(
                        prepared.runtimeConfig(),
                        prepared.turns(),
                        prepared.tools(),
                        toolCall -> executeToolWithTrace(execution, prepared, resolvedTools, toolCall),
                        prepared.temperature(),
                        prepared.topP(),
                        prepared.maxTokens());
            } else {
                content = chatModelGateway.chat(
                        prepared.runtimeConfig(),
                        prepared.turns(),
                        prepared.temperature(),
                        prepared.topP(),
                        prepared.maxTokens());
            }
            touchCredential(prepared);
            quotaApplicationService.consumeAiUsage(workspaceId, estimateTokens(prepared, content));
            return content;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.EXECUTION_FAILED, "模型调用失败");
        }
    }

    public SseEmitter stream(PreparedAgentChat prepared) {
        return stream(prepared, null);
    }

    public SseEmitter stream(PreparedAgentChat prepared, Consumer<String> onCompleted) {
        return stream(prepared, onCompleted, null);
    }

    public SseEmitter stream(PreparedAgentChat prepared, Consumer<String> onCompleted, Long executionId) {
        return stream(prepared, onCompleted, executionId, null);
    }

    public SseEmitter stream(PreparedAgentChat prepared,
                             Consumer<String> onCompleted,
                             Long executionId,
                             String citationsJson) {
        return stream(prepared, onCompleted, executionId, citationsJson, null);
    }

    public SseEmitter stream(PreparedAgentChat prepared,
                             Consumer<String> onCompleted,
                             Long executionId,
                             String citationsJson,
                             Execution execution) {
        Long workspaceId = WorkspaceContext.require().workspaceId();
        assertChatRateLimit();
        quotaApplicationService.assertAiQuotaAvailable(workspaceId);
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        emitter.onTimeout(emitter::complete);
        StringBuilder contentBuilder = new StringBuilder();
        WorkspaceContext workspaceContext = WorkspaceContext.get();
        SecurityContext securityContext = SecurityContextHolder.getContext();
        CompletableFuture.runAsync(() -> {
            if (workspaceContext != null) {
                WorkspaceContext.set(workspaceContext);
            }
            SecurityContextHolder.setContext(securityContext);
            try {
                if (citationsJson != null && !citationsJson.isBlank()) {
                    sendStreamEvent(emitter, ChatStreamEvent.citations(citationsJson));
                }
                if (prepared.tools() != null && !prepared.tools().isEmpty()) {
                    streamWithTools(emitter, prepared, onCompleted, executionId, execution, workspaceId, contentBuilder);
                    return;
                }
                chatModelGateway.streamChat(
                        prepared.runtimeConfig(),
                        prepared.turns(),
                        prepared.temperature(),
                        prepared.topP(),
                        prepared.maxTokens(),
                        new ChatStreamHandler() {
                            @Override
                            public void onPartial(String partial) {
                                contentBuilder.append(partial);
                                try {
                                    sendStreamEvent(emitter, ChatStreamEvent.delta(partial));
                                } catch (IOException e) {
                                    emitter.completeWithError(e);
                                }
                            }

                            @Override
                            public void onComplete() {
                                try {
                                    touchCredential(prepared);
                                    String content = contentBuilder.toString();
                                    quotaApplicationService.consumeAiUsage(workspaceId, estimateTokens(prepared, content));
                                    if (onCompleted != null) {
                                        onCompleted.accept(content);
                                    }
                                    sendStreamEvent(emitter, executionId == null
                                            ? ChatStreamEvent.done()
                                            : ChatStreamEvent.done(executionId));
                                    emitter.complete();
                                } catch (Exception e) {
                                    emitter.completeWithError(e);
                                }
                            }

                            @Override
                            public void onError(Throwable error) {
                                completeStreamWithError(emitter, error);
                            }
                        });
            } catch (BusinessException e) {
                completeStreamWithError(emitter, e);
            } catch (Exception e) {
                completeStreamWithError(emitter, new BusinessException(ErrorCode.EXECUTION_FAILED, "模型调用失败"));
            } finally {
                WorkspaceContext.clear();
                SecurityContextHolder.clearContext();
            }
        }, STREAM_EXECUTOR);
        return emitter;
    }

    private void streamWithTools(SseEmitter emitter,
                                 PreparedAgentChat prepared,
                                 Consumer<String> onCompleted,
                                 Long executionId,
                                 Execution execution,
                                 Long workspaceId,
                                 StringBuilder contentBuilder) throws IOException {
        List<ResolvedAgentTool> resolvedTools = agentToolRuntimeService.resolveTools(prepared.agentVersionId());
        try {
            chatModelGateway.streamChatWithTools(
                    prepared.runtimeConfig(),
                    prepared.turns(),
                    prepared.tools(),
                    toolCall -> {
                        try {
                            sendStreamEvent(emitter, ChatStreamEvent.toolStart(toolPayload(
                                    toolCall.toolKey(),
                                    toolCall.arguments(),
                                    null,
                                    null)));
                            String result = executeToolWithTrace(execution, prepared, resolvedTools, toolCall);
                            sendStreamEvent(emitter, ChatStreamEvent.toolDelta(toolPayload(
                                    toolCall.toolKey(),
                                    toolCall.arguments(),
                                    result,
                                    null)));
                            sendStreamEvent(emitter, ChatStreamEvent.toolEnd(toolPayload(
                                    toolCall.toolKey(),
                                    toolCall.arguments(),
                                    result,
                                    "SUCCEEDED")));
                            return result;
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        } catch (RuntimeException e) {
                            try {
                                sendStreamEvent(emitter, ChatStreamEvent.toolEnd(toolPayload(
                                        toolCall.toolKey(),
                                        toolCall.arguments(),
                                        null,
                                        e.getMessage())));
                            } catch (IOException ioException) {
                                throw new RuntimeException(ioException);
                            }
                            throw e;
                        }
                    },
                    prepared.temperature(),
                    prepared.topP(),
                    prepared.maxTokens(),
                    new ChatStreamHandler() {
                        @Override
                        public void onPartial(String partial) {
                            contentBuilder.append(partial);
                            try {
                                sendStreamEvent(emitter, ChatStreamEvent.delta(partial));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        @Override
                        public void onComplete() {
                            try {
                                touchCredential(prepared);
                                String content = contentBuilder.toString();
                                quotaApplicationService.consumeAiUsage(workspaceId, estimateTokens(prepared, content));
                                if (onCompleted != null) {
                                    onCompleted.accept(content);
                                }
                                sendStreamEvent(emitter, executionId == null
                                        ? ChatStreamEvent.done()
                                        : ChatStreamEvent.done(executionId));
                                emitter.complete();
                            } catch (Exception e) {
                                emitter.completeWithError(e);
                            }
                        }

                        @Override
                        public void onError(Throwable error) {
                            completeStreamWithError(emitter, error);
                        }
                    },
                    null);
        } catch (ToolConfirmationRequiredException confirmation) {
            sendStreamEvent(emitter, ChatStreamEvent.toolConfirmRequired(confirmPayload(confirmation)));
            sendStreamEvent(emitter, executionId == null ? ChatStreamEvent.done() : ChatStreamEvent.done(executionId));
            emitter.complete();
        }
    }

    private String toolPayload(String toolKey,
                               Map<String, Object> arguments,
                               String output,
                               String status) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("toolKey", toolKey);
        if (arguments != null && !arguments.isEmpty()) {
            payload.put("arguments", arguments);
        }
        if (output != null) {
            payload.put("output", output);
        }
        if (status != null) {
            payload.put("status", status);
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return "{\"toolKey\":\"" + toolKey + "\"}";
        }
    }

    private void sendStreamEvent(SseEmitter emitter, ChatStreamEvent event) throws IOException {
        emitter.send(SseEmitter.event().data(event));
    }

    private void touchCredential(PreparedAgentChat prepared) {
        if (prepared.platformCredential()) {
            platformModelApplicationService.touchCredential(prepared.credentialId());
        } else {
            credentialRepository.touchLastUsed(prepared.credentialId());
        }
    }

    private long estimateTokens(PreparedAgentChat prepared, String response) {
        long total = response == null ? 0 : response.length();
        if (prepared.turns() != null) {
            for (var turn : prepared.turns()) {
                if (turn.content() != null) {
                    total += turn.content().length();
                }
            }
        }
        return Math.max(total, 1L);
    }

    private String executeToolWithTrace(Execution execution,
                                        PreparedAgentChat prepared,
                                        List<ResolvedAgentTool> tools,
                                        ToolCall toolCall) {
        String toolKey = toolCall.toolKey();
        ResolvedAgentTool resolved = tools.stream()
                .filter(item -> item.toolKey().equals(toolKey))
                .findFirst()
                .orElse(null);
        if (resolved != null && resolved.requireConfirmation()) {
            String token = prepared.toolConfirmationToken();
            if (token == null || token.isBlank()) {
                String confirmationToken = toolConfirmationService.create(new ToolConfirmationService.PendingConfirmation(
                        WorkspaceContext.require().userId(),
                        WorkspaceContext.require().workspaceId(),
                        prepared.agentId(),
                        prepared.agentVersionId(),
                        toolKey,
                        resolved.name(),
                        toolCall.arguments()));
                throw new ToolConfirmationRequiredException(
                        confirmationToken,
                        toolKey,
                        resolved.name(),
                        toolCall.arguments());
            }
            toolConfirmationService.consume(token, WorkspaceContext.require().userId(), WorkspaceContext.require().workspaceId(), toolKey);
        }
        Map<String, Object> input = toolCall.arguments() == null || toolCall.arguments().isEmpty()
                ? Map.of("toolKey", toolKey)
                : Map.of("toolKey", toolKey, "arguments", toolCall.arguments());
        try {
            String result = agentToolRuntimeService.executeByKey(tools, toolKey, toolCall.arguments());
            if (execution != null) {
                executionRecorder.recordToolSpan(execution, toolKey, input, result);
            }
            return result;
        } catch (RuntimeException e) {
            if (execution != null) {
                executionRecorder.recordToolSpanFailed(execution, toolKey, input, e.getMessage());
            }
            throw e;
        }
    }

    private String confirmPayload(ToolConfirmationRequiredException confirmation) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("confirmationToken", confirmation.confirmationToken());
        payload.put("toolKey", confirmation.toolKey());
        payload.put("toolName", confirmation.toolName());
        if (confirmation.arguments() != null && !confirmation.arguments().isEmpty()) {
            payload.put("arguments", confirmation.arguments());
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return "{\"toolKey\":\"" + confirmation.toolKey() + "\"}";
        }
    }

    private void completeStreamWithError(SseEmitter emitter, Throwable error) {
        String message = error instanceof BusinessException businessException
                ? businessException.getMessage()
                : "模型调用失败";
        try {
            sendStreamEvent(emitter, ChatStreamEvent.error(message));
        } catch (IOException ignored) {
            // ignore secondary send failure
        }
        emitter.completeWithError(error);
    }
}
