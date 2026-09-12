package com.boxai.knowledge.controller;

import com.boxai.common.result.Result;
import com.boxai.knowledge.api.KnowledgeChunkVO;
import com.boxai.knowledge.api.KnowledgeDocumentVO;
import com.boxai.knowledge.application.KnowledgeDocumentApplicationService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
public class KnowledgeDocumentController {

    private final KnowledgeDocumentApplicationService knowledgeDocumentApplicationService;

    public KnowledgeDocumentController(KnowledgeDocumentApplicationService knowledgeDocumentApplicationService) {
        this.knowledgeDocumentApplicationService = knowledgeDocumentApplicationService;
    }

    @GetMapping("/{id}")
    public Result<KnowledgeDocumentVO> detail(@PathVariable Long id) {
        return Result.success(knowledgeDocumentApplicationService.detail(id));
    }

    @GetMapping("/{id}/chunks")
    public Result<List<KnowledgeChunkVO>> listChunks(@PathVariable Long id) {
        return Result.success(knowledgeDocumentApplicationService.listChunks(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        knowledgeDocumentApplicationService.delete(id);
        return Result.success();
    }
}
