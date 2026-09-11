package com.boxai.agent.chat;

import com.boxai.agent.api.ChatStreamEvent;
import com.boxai.ai.ChatModelGateway;
import com.boxai.ai.ChatStreamHandler;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.model.ModelCredentialRepository;
import com.boxai.model.application.PlatformModelApplicationService;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.tenant.application.QuotaApplicationService;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Service
public class AgentChatExecutor {

    private static final long STREAM_TIMEOUT_MS = 120_000L;

    private final ChatModelGateway chatModelGateway;
    private final ModelCredentialRepository credentialRepository;
    private final QuotaApplicationService quotaApplicationService;
    private final PlatformModelApplicationService platformModelApplicationService;
    private final AgentToolRuntimeService agentToolRuntimeService;

    public AgentChatExecutor(ChatModelGateway chatModelGateway,
                             ModelCredentialRepository credentialRepository,
                             QuotaApplicationService quotaApplicationService,
                             PlatformModelApplicationService platformModelApplicationService,
                             AgentToolRuntimeService agentToolRuntimeService) {
        this.chatModelGateway = chatModelGateway;
        this.credentialRepository = credentialRepository;
        this.quotaApplicationService = quotaApplicationService;
        this.platformModelApplicationService = platformModelApplicationService;
        this.agentToolRuntimeService = agentToolRuntimeService;
    }

    public void assertQuotaAvailable() {
        quotaApplicationService.assertAiQuotaAvailable(WorkspaceContext.require().workspaceId());
    }

    public String chat(PreparedAgentChat prepared) {
        Long workspaceId = WorkspaceContext.require().workspaceId();
        quotaApplicationService.assertAiQuotaAvailable(workspaceId);
        try {
            String content;
            if (prepared.tools() != null && !prepared.tools().isEmpty()) {
                List<ResolvedAgentTool> resolvedTools = agentToolRuntimeService.resolveTools(prepared.agentVersionId());
                content = chatModelGateway.chatWithTools(
                        prepared.runtimeConfig(),
                        prepared.turns(),
                        prepared.tools(),
                        toolKey -> agentToolRuntimeService.executeByKey(resolvedTools, toolKey),
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
        Long workspaceId = WorkspaceContext.require().workspaceId();
        quotaApplicationService.assertAiQuotaAvailable(workspaceId);
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        emitter.onTimeout(emitter::complete);
        StringBuilder contentBuilder = new StringBuilder();
        CompletableFuture.runAsync(() -> {
            try {
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
                                    sendStreamEvent(emitter, ChatStreamEvent.done());
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
            }
        });
        return emitter;
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
