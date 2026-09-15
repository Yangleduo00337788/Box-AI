package com.boxai.knowledge.controller;

import com.boxai.common.result.Result;
import com.boxai.knowledge.api.CreateKnowledgeBaseRequest;
import com.boxai.knowledge.api.ImportKnowledgeUrlRequest;
import com.boxai.knowledge.api.KnowledgeBaseVO;
import com.boxai.knowledge.api.KnowledgeDocumentVO;
import com.boxai.knowledge.api.KnowledgeSearchHitVO;
import com.boxai.knowledge.api.KnowledgeSearchRequest;
import com.boxai.knowledge.api.KnowledgeTestAnswerRequest;
import com.boxai.knowledge.api.KnowledgeTestAnswerVO;
import com.boxai.knowledge.api.UpdateKnowledgeBaseRequest;
import com.boxai.knowledge.application.KnowledgeBaseApplicationService;
import com.boxai.knowledge.application.KnowledgeDocumentApplicationService;
import com.boxai.knowledge.application.KnowledgeSearchService;
import com.boxai.knowledge.application.KnowledgeTestAnswerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge-bases")
public class KnowledgeBaseController {

    private final KnowledgeBaseApplicationService knowledgeBaseApplicationService;
    private final KnowledgeDocumentApplicationService knowledgeDocumentApplicationService;
    private final KnowledgeSearchService knowledgeSearchService;
    private final KnowledgeTestAnswerService knowledgeTestAnswerService;

    public KnowledgeBaseController(KnowledgeBaseApplicationService knowledgeBaseApplicationService,
                                   KnowledgeDocumentApplicationService knowledgeDocumentApplicationService,
                                   KnowledgeSearchService knowledgeSearchService,
                                   KnowledgeTestAnswerService knowledgeTestAnswerService) {
        this.knowledgeBaseApplicationService = knowledgeBaseApplicationService;
        this.knowledgeDocumentApplicationService = knowledgeDocumentApplicationService;
        this.knowledgeSearchService = knowledgeSearchService;
        this.knowledgeTestAnswerService = knowledgeTestAnswerService;
    }

    @GetMapping
    public Result<List<KnowledgeBaseVO>> list() {
        return Result.success(knowledgeBaseApplicationService.list());
    }

    @PostMapping
    public Result<KnowledgeBaseVO> create(@Valid @RequestBody CreateKnowledgeBaseRequest request) {
        return Result.success(knowledgeBaseApplicationService.create(request));
    }

    @GetMapping("/{id}")
    public Result<KnowledgeBaseVO> detail(@PathVariable Long id) {
        return Result.success(knowledgeBaseApplicationService.detail(id));
    }

    @PutMapping("/{id}")
    public Result<KnowledgeBaseVO> update(@PathVariable Long id,
                                          @Valid @RequestBody UpdateKnowledgeBaseRequest request) {
        return Result.success(knowledgeBaseApplicationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        knowledgeBaseApplicationService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}/documents")
    public Result<List<KnowledgeDocumentVO>> listDocuments(@PathVariable Long id) {
        return Result.success(knowledgeDocumentApplicationService.list(id));
    }

    @PostMapping("/{id}/documents")
    public Result<KnowledgeDocumentVO> uploadDocument(@PathVariable Long id,
                                                      @RequestPart("file") MultipartFile file) {
        return Result.success(knowledgeDocumentApplicationService.upload(id, file));
    }

    @PostMapping("/{id}/documents/import-url")
    public Result<KnowledgeDocumentVO> importUrl(@PathVariable Long id,
                                                 @Valid @RequestBody ImportKnowledgeUrlRequest request) {
        return Result.success(knowledgeDocumentApplicationService.importFromUrl(
                id, request.url(), request.syncCron()));
    }

    @PostMapping("/{id}/search")
    public Result<List<KnowledgeSearchHitVO>> search(@PathVariable Long id,
                                                    @Valid @RequestBody KnowledgeSearchRequest request) {
        return Result.success(knowledgeSearchService.search(id, request));
    }

    @PostMapping("/{id}/test-answer")
    public Result<KnowledgeTestAnswerVO> testAnswer(@PathVariable Long id,
                                                    @Valid @RequestBody KnowledgeTestAnswerRequest request) {
        return Result.success(knowledgeTestAnswerService.testAnswer(id, request));
    }
}
