package com.boxai.knowledge.application;

import com.boxai.domain.knowledge.KnowledgeBase;
import com.boxai.domain.knowledge.KnowledgeBaseRepository;
import com.boxai.domain.knowledge.KnowledgeChunk;
import com.boxai.domain.knowledge.KnowledgeChunkRepository;
import com.boxai.domain.knowledge.KnowledgeDocument;
import com.boxai.domain.knowledge.KnowledgeDocumentRepository;
import com.boxai.domain.storage.ObjectStorage;
import com.boxai.knowledge.support.DocumentTextExtractor;
import com.boxai.knowledge.support.ExtractedTextValidator;
import com.boxai.knowledge.support.KnowledgeDocumentProgress;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.notification.NotificationPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Service
public class KnowledgeDocumentProcessingService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeDocumentProcessingService.class);
    private static final int CHUNK_SIZE = 800;
    private static final int CHUNK_OVERLAP = 100;
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "svg", "gif", "webp", "bmp");

    private final Executor knowledgeDocumentExecutor;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final ObjectStorage objectStorage;
    private final KnowledgeChunkIndexingService chunkIndexingService;
    private final DocumentTextExtractor documentTextExtractor;
    private final KnowledgeOcrService knowledgeOcrService;
    private final NotificationPublisher notificationPublisher;
    private final TransactionTemplate transactionTemplate;
    private final ConcurrentHashMap<Long, Object> documentProcessingLocks = new ConcurrentHashMap<>();

    public KnowledgeDocumentProcessingService(@Qualifier("knowledgeDocumentExecutor") Executor knowledgeDocumentExecutor,
                                              KnowledgeDocumentRepository knowledgeDocumentRepository,
                                              KnowledgeBaseRepository knowledgeBaseRepository,
                                              KnowledgeChunkRepository knowledgeChunkRepository,
                                              ObjectStorage objectStorage,
                                              KnowledgeChunkIndexingService chunkIndexingService,
                                              DocumentTextExtractor documentTextExtractor,
                                              KnowledgeOcrService knowledgeOcrService,
                                              NotificationPublisher notificationPublisher,
                                              TransactionTemplate transactionTemplate) {
        this.knowledgeDocumentExecutor = knowledgeDocumentExecutor;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.knowledgeChunkRepository = knowledgeChunkRepository;
        this.objectStorage = objectStorage;
        this.chunkIndexingService = chunkIndexingService;
        this.documentTextExtractor = documentTextExtractor;
        this.knowledgeOcrService = knowledgeOcrService;
        this.notificationPublisher = notificationPublisher;
        this.transactionTemplate = transactionTemplate;
    }

    public void enqueue(Long documentId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    submit(documentId);
                }
            });
            log.info("Knowledge document {} will be processed after commit", documentId);
            return;
        }
        submit(documentId);
    }

    private void submit(Long documentId) {
        WorkspaceContext workspaceContext = WorkspaceContext.get();
        SecurityContext securityContext = SecurityContextHolder.getContext();
        log.info("Knowledge document {} queued for processing", documentId);
        knowledgeDocumentExecutor.execute(() -> {
            if (workspaceContext != null) {
                WorkspaceContext.set(workspaceContext);
            }
            SecurityContextHolder.setContext(securityContext);
            Object lock = documentProcessingLocks.computeIfAbsent(documentId, ignored -> new Object());
            synchronized (lock) {
                try {
                    log.info("Knowledge document {} processing started on {}", documentId, Thread.currentThread().getName());
                    processSafely(documentId);
                } finally {
                    documentProcessingLocks.remove(documentId, lock);
                    WorkspaceContext.clear();
                    SecurityContextHolder.clearContext();
                }
            }
        });
    }

    public void deleteDocumentIndex(Long documentId) {
        chunkIndexingService.deleteDocumentIndex(documentId);
    }

    private void processSafely(Long documentId) {
        try {
            process(documentId);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().startsWith("document not found:")) {
                log.warn("Skip processing for missing document {}: {}", documentId, e.getMessage());
                return;
            }
            log.warn("Knowledge document {} processing failed: {}", documentId, e.getMessage());
            transactionTemplate.executeWithoutResult(status -> {
                KnowledgeDocument document = knowledgeDocumentRepository.findById(documentId).orElse(null);
                if (document != null && !"READY".equals(document.getStatus())) {
                    markFailed(document, "文档处理失败");
                }
            });
        }
    }

    public void process(Long documentId) {
        KnowledgeDocument document = requireDocumentForProcessing(documentId);
        if ("READY".equals(document.getStatus())) {
            return;
        }
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(document.getKnowledgeBaseId())
                .orElseThrow(() -> new IllegalStateException("knowledge base not found"));
        if (document.getStorageBucket() == null || document.getStorageKey() == null) {
            markFailed(document, "文档存储信息缺失");
            return;
        }

        updateStatus(document.getId(), "PARSING", KnowledgeDocumentProgress.PARSING);
        log.info("Knowledge document {}: reading from storage", documentId);
        byte[] bytes;
        try (var input = objectStorage.get(document.getStorageBucket(), document.getStorageKey())) {
            bytes = input.readAllBytes();
        } catch (Exception e) {
            markFailed(document.getId(), "读取文档失败");
            return;
        }

        String text;
        try {
            text = documentTextExtractor.extract(bytes, document.getFileName());
        } catch (Exception e) {
            markFailed(document.getId(), "文档解析失败");
            return;
        }
        if (ExtractedTextValidator.looksLikeBinaryNoise(text)) {
            text = "";
        }

        if (knowledgeOcrService.mayNeedOcr(document.getFileName(), document.getMimeType(), text)) {
            updateStatus(document.getId(), "OCR", KnowledgeDocumentProgress.OCR);
            log.info("Knowledge document {}: OCR started for {}", documentId, document.getFileName());
            text = knowledgeOcrService.enrichExtractedText(knowledgeBase, bytes, document.getFileName(), document.getMimeType(), text);
            log.info("Knowledge document {}: OCR finished, text length={}", documentId, text == null ? 0 : text.length());
        }

        if (text == null || text.isBlank()) {
            String message = isImageDocument(document.getFileName())
                    ? "图片未能识别出文字，请在管理端配置可用的平台 OCR/视觉模型后重试"
                    : "未能从文档中提取到可用文本";
            markFailed(document.getId(), message);
            return;
        }
        if (ExtractedTextValidator.looksLikeBinaryNoise(text)) {
            String message = isImageDocument(document.getFileName())
                    ? "图片解析结果异常，请确认平台 OCR/视觉模型配置正确后重试"
                    : "文档解析结果异常，请检查文件内容后重试";
            markFailed(document.getId(), message);
            return;
        }

        KnowledgeDocument fresh = knowledgeDocumentRepository.findById(documentId).orElse(document);
        processChunksAndIndex(knowledgeBase, fresh, text);
        refreshKnowledgeBaseCounts(knowledgeBase.getId());
    }

    private void processChunksAndIndex(KnowledgeBase knowledgeBase, KnowledgeDocument document, String content) {
        updateStatus(document.getId(), "CHUNKING", KnowledgeDocumentProgress.CHUNKING);
        knowledgeChunkRepository.deleteByDocument(document.getId());
        chunkIndexingService.deleteDocumentIndex(document.getId());
        List<String> parts = splitText(content);
        List<KnowledgeChunk> chunks = new ArrayList<>();
        for (int i = 0; i < parts.size(); i++) {
            String part = parts.get(i);
            if (part.isBlank()) {
                continue;
            }
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setWorkspaceId(document.getWorkspaceId());
            chunk.setKnowledgeBaseId(document.getKnowledgeBaseId());
            chunk.setDocumentId(document.getId());
            chunk.setChunkIndex(i);
            chunk.setContent(part);
            chunk.setTokenCount(part.length());
            chunk.setEsDocumentId(UUID.randomUUID().toString().replace("-", ""));
            chunk.setStatus("INDEXED");
            chunks.add(chunk);
        }
        transactionTemplate.executeWithoutResult(status -> knowledgeChunkRepository.saveBatch(chunks));

        updateStatus(document.getId(), "EMBEDDING", KnowledgeDocumentProgress.EMBEDDING);
        updateStatus(document.getId(), "INDEXING", KnowledgeDocumentProgress.INDEXING);
        chunkIndexingService.indexChunks(knowledgeBase, chunks);

        transactionTemplate.executeWithoutResult(status -> {
            KnowledgeDocument row = knowledgeDocumentRepository.findById(document.getId()).orElse(document);
            row.setChunkCount(chunks.size());
            row.setStatus("READY");
            row.setProgress(KnowledgeDocumentProgress.READY);
            row.setErrorMessage(null);
            knowledgeDocumentRepository.update(row);
            notifyDocumentProcessed(row, true, null);
        });
    }

    private KnowledgeDocument requireDocumentForProcessing(Long documentId) {
        for (int attempt = 1; attempt <= 8; attempt++) {
            KnowledgeDocument document = knowledgeDocumentRepository.findById(documentId).orElse(null);
            if (document != null) {
                return document;
            }
            try {
                Thread.sleep(50L * attempt);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new IllegalStateException("document not found: " + documentId);
    }

    private void updateStatus(Long documentId, String status, int progress) {
        transactionTemplate.executeWithoutResult(tx -> {
            KnowledgeDocument row = knowledgeDocumentRepository.findById(documentId).orElse(null);
            if (row == null) {
                return;
            }
            row.setStatus(status);
            row.setProgress(progress);
            row.setErrorMessage(null);
            knowledgeDocumentRepository.update(row);
        });
    }

    private void markFailed(Long documentId, String message) {
        KnowledgeDocument document = knowledgeDocumentRepository.findById(documentId).orElse(null);
        if (document != null) {
            markFailed(document, message);
        }
    }

    private void markFailed(KnowledgeDocument document, String message) {
        transactionTemplate.executeWithoutResult(status -> {
            document.setStatus("FAILED");
            if (document.getProgress() == null) {
                document.setProgress(0);
            }
            document.setErrorMessage(message);
            knowledgeDocumentRepository.update(document);
            notifyDocumentProcessed(document, false, message);
        });
    }

    private void refreshKnowledgeBaseCounts(Long knowledgeBaseId) {
        transactionTemplate.executeWithoutResult(status -> {
            KnowledgeBase kb = knowledgeBaseRepository.findById(knowledgeBaseId).orElse(null);
            if (kb == null) {
                return;
            }
            kb.setDocumentCount(knowledgeDocumentRepository.listByKnowledgeBase(kb.getId()).size());
            kb.setChunkCount((long) knowledgeChunkRepository.countByKnowledgeBase(kb.getId()));
            knowledgeBaseRepository.update(kb);
        });
    }

    private void notifyDocumentProcessed(KnowledgeDocument document, boolean success, String errorMessage) {
        Long userId = document.getCreatedBy();
        if (userId == null) {
            return;
        }
        String title = success ? "知识库文档处理完成" : "知识库文档处理失败";
        String content = success
                ? "文档「" + document.getName() + "」已索引完成，可用于检索。"
                : "文档「" + document.getName() + "」处理失败：" + (errorMessage == null ? "未知错误" : errorMessage);
        notificationPublisher.publish(
                userId,
                document.getWorkspaceId(),
                title,
                content,
                "KNOWLEDGE",
                "/knowledge");
    }

    private boolean isImageDocument(String fileName) {
        if (fileName == null) {
            return false;
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return false;
        }
        return IMAGE_EXTENSIONS.contains(fileName.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    private List<String> splitText(String content) {
        List<String> parts = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return parts;
        }
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(content.length(), start + CHUNK_SIZE);
            parts.add(content.substring(start, end));
            if (end >= content.length()) {
                break;
            }
            start = Math.max(end - CHUNK_OVERLAP, start + 1);
        }
        return parts;
    }
}
