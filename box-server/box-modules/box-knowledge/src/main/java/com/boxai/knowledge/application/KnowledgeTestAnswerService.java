package com.boxai.knowledge.application;

import com.boxai.ai.ChatModelGateway;
import com.boxai.ai.ModelRuntimeConfig;
import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.knowledge.KnowledgeBase;
import com.boxai.knowledge.api.KnowledgeSearchHitVO;
import com.boxai.knowledge.api.KnowledgeTestAnswerRequest;
import com.boxai.knowledge.api.KnowledgeTestAnswerVO;
import com.boxai.model.application.PlatformModelApplicationService;
import com.boxai.model.platform.ResolvedPlatformModel;
import com.boxai.security.permission.WorkspacePermissionService;
import com.boxai.tenant.application.QuotaApplicationService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class KnowledgeTestAnswerService {

    private static final String SYSTEM_PROMPT = """
            你是知识库问答助手。请仅根据提供的上下文回答问题。
            如果上下文不足以回答，请明确说明无法从知识库中找到答案。
            回答时可使用 [1]、[2] 标注引用来源。
            """;

    private final KnowledgeBaseApplicationService knowledgeBaseApplicationService;
    private final KnowledgeSearchService knowledgeSearchService;
    private final ChatModelGateway chatModelGateway;
    private final PlatformModelApplicationService platformModelApplicationService;
    private final WorkspacePermissionService workspacePermissionService;
    private final QuotaApplicationService quotaApplicationService;

    public KnowledgeTestAnswerService(KnowledgeBaseApplicationService knowledgeBaseApplicationService,
                                      KnowledgeSearchService knowledgeSearchService,
                                      ChatModelGateway chatModelGateway,
                                      PlatformModelApplicationService platformModelApplicationService,
                                      WorkspacePermissionService workspacePermissionService,
                                      QuotaApplicationService quotaApplicationService) {
        this.knowledgeBaseApplicationService = knowledgeBaseApplicationService;
        this.knowledgeSearchService = knowledgeSearchService;
        this.chatModelGateway = chatModelGateway;
        this.platformModelApplicationService = platformModelApplicationService;
        this.workspacePermissionService = workspacePermissionService;
        this.quotaApplicationService = quotaApplicationService;
    }

    public KnowledgeTestAnswerVO testAnswer(Long knowledgeBaseId, KnowledgeTestAnswerRequest request) {
        workspacePermissionService.requirePermission(PermissionCodes.KNOWLEDGE_READ);
        KnowledgeBase knowledgeBase = knowledgeBaseApplicationService.requireKnowledgeBase(knowledgeBaseId);
        int topK = request.topK() == null ? 5 : request.topK();
        boolean rerank = knowledgeBase.getRerankModelId() != null;
        List<KnowledgeSearchHitVO> citations = knowledgeSearchService.searchHits(
                knowledgeBaseId,
                request.query(),
                topK,
                "HYBRID",
                null,
                rerank);
        if (citations.isEmpty()) {
            return new KnowledgeTestAnswerVO("知识库中未检索到相关内容，无法生成回答。", List.of());
        }

        String context = buildContext(citations);
        ModelRuntimeConfig chatConfig = resolveChatConfig(knowledgeBase.getWorkspaceId());
        quotaApplicationService.assertAiQuotaAvailable(knowledgeBase.getWorkspaceId());
        String answer = chatModelGateway.chat(
                chatConfig,
                SYSTEM_PROMPT,
                "问题：\n" + request.query().trim() + "\n\n上下文：\n" + context,
                0.2,
                1.0,
                1024);
        quotaApplicationService.consumeAiUsage(
                knowledgeBase.getWorkspaceId(),
                Math.max(request.query().length(), 1L));
        return new KnowledgeTestAnswerVO(answer, citations);
    }

    private String buildContext(List<KnowledgeSearchHitVO> citations) {
        List<String> parts = new ArrayList<>();
        int index = 1;
        for (KnowledgeSearchHitVO citation : citations) {
            parts.add("[" + index + "]\n" + citation.content());
            index++;
        }
        return String.join("\n\n", parts);
    }

    private ModelRuntimeConfig resolveChatConfig(Long workspaceId) {
        Long modelId = platformModelApplicationService.findFirstRunnableModelId().orElse(null);
        if (modelId == null) {
            throw new BusinessException(ErrorCode.MODEL_NOT_FOUND, "未配置可用平台模型");
        }
        ResolvedPlatformModel resolved = platformModelApplicationService.resolveForChat(modelId);
        quotaApplicationService.assertAiQuotaAvailable(workspaceId);
        return resolved.runtimeConfig();
    }
}
