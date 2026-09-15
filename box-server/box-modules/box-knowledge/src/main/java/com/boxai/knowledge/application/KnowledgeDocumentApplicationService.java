package com.boxai.knowledge.application;

import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.knowledge.KnowledgeBase;
import com.boxai.domain.knowledge.KnowledgeBaseRepository;
import com.boxai.domain.knowledge.KnowledgeChunk;
import com.boxai.domain.knowledge.KnowledgeChunkRepository;
import com.boxai.domain.knowledge.KnowledgeDocument;
import com.boxai.domain.knowledge.KnowledgeDocumentRepository;
import com.boxai.domain.storage.ObjectStorage;
import com.boxai.knowledge.api.KnowledgeChunkVO;
import com.boxai.knowledge.api.KnowledgeDocumentVO;
import com.boxai.knowledge.support.DocumentTextExtractor;
import com.boxai.common.security.FileSafetyPolicy;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.notification.NotificationPublisher;
import com.boxai.security.permission.WorkspacePermissionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class KnowledgeDocumentApplicationService {

    private static final int CHUNK_SIZE = 800;
    private static final int CHUNK_OVERLAP = 100;

    private final KnowledgeBaseApplicationService knowledgeBaseApplicationService;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final ObjectStorage objectStorage;
    private final String storageBucket;
    private final KnowledgeChunkIndexingService chunkIndexingService;
    private final DocumentTextExtractor documentTextExtractor;
    private final WorkspacePermissionService workspacePermissionService;
    private final NotificationPublisher notificationPublisher;

    public KnowledgeDocumentApplicationService(KnowledgeBaseApplicationService knowledgeBaseApplicationService,
                                               KnowledgeBaseRepository knowledgeBaseRepository,
                                               KnowledgeDocumentRepository knowledgeDocumentRepository,
                                               KnowledgeChunkRepository knowledgeChunkRepository,
                                               ObjectStorage objectStorage,
                                               KnowledgeChunkIndexingService chunkIndexingService,
                                               DocumentTextExtractor documentTextExtractor,
                                               WorkspacePermissionService workspacePermissionService,
                                               NotificationPublisher notificationPublisher,
                                               @Value("${box.minio.bucket:box}") String storageBucket) {
        this.knowledgeBaseApplicationService = knowledgeBaseApplicationService;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.knowledgeChunkRepository = knowledgeChunkRepository;
        this.objectStorage = objectStorage;
        this.chunkIndexingService = chunkIndexingService;
        this.documentTextExtractor = documentTextExtractor;
        this.workspacePermissionService = workspacePermissionService;
        this.notificationPublisher = notificationPublisher;
        this.storageBucket = storageBucket;
    }

    public List<KnowledgeDocumentVO> list(Long knowledgeBaseId) {
        workspacePermissionService.requirePermission(PermissionCodes.KNOWLEDGE_READ);
        knowledgeBaseApplicationService.requireKnowledgeBase(knowledgeBaseId);
        return knowledgeDocumentRepository.listByKnowledgeBase(knowledgeBaseId).stream().map(this::toVO).toList();
    }

    public KnowledgeDocumentVO detail(Long documentId) {
        workspacePermissionService.requirePermission(PermissionCodes.KNOWLEDGE_READ);
        return toVO(requireDocument(documentId));
    }

    public List<KnowledgeChunkVO> listChunks(Long documentId) {
        workspacePermissionService.requirePermission(PermissionCodes.KNOWLEDGE_READ);
        requireDocument(documentId);
        return knowledgeChunkRepository.listByDocument(documentId).stream().map(this::toChunkVO).toList();
    }

    @Transactional
    public KnowledgeDocumentVO upload(Long knowledgeBaseId, MultipartFile file) {
        workspacePermissionService.requirePermission(PermissionCodes.KNOWLEDGE_UPLOAD);
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请上传文件");
        }
        KnowledgeBase kb = knowledgeBaseApplicationService.requireKnowledgeBase(knowledgeBaseId);
        Long userId = WorkspaceContext.require().userId();
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "读取上传文件失败");
        }
        String originalName = FileSafetyPolicy.sanitizeFileName(
                file.getOriginalFilename() == null ? "document.txt" : file.getOriginalFilename());
        FileSafetyPolicy.validate(originalName, file.getContentType(), file.getSize(), bytes);
        String fileType = resolveFileType(originalName);

        KnowledgeDocument document = new KnowledgeDocument();
        document.setWorkspaceId(kb.getWorkspaceId());
        document.setKnowledgeBaseId(kb.getId());
        document.setName(originalName);
        document.setFileName(originalName);
        document.setFileType(fileType);
        document.setMimeType(file.getContentType());
        document.setFileSize(file.getSize());
        document.setStorageBucket(storageBucket);
        document.setChunkCount(0);
        document.setStatus("UPLOADING");
        document.setCreatedBy(userId);
        knowledgeDocumentRepository.save(document);

        String storageKey = "knowledge/" + kb.getId() + "/" + document.getId() + "/" + originalName;
        document.setStorageKey(storageKey);
        try {
            document.setMd5(md5(bytes));
            objectStorage.put(storageBucket, storageKey, new java.io.ByteArrayInputStream(bytes), bytes.length, file.getContentType());
            document.setStatus("PARSING");
            knowledgeDocumentRepository.update(document);
            String text = documentTextExtractor.extract(bytes, originalName);
            processDocument(kb, document, text);
            knowledgeDocumentRepository.update(document);
            refreshKnowledgeBaseCounts(kb);
            return toVO(document);
        } catch (BusinessException e) {
            markFailed(document, e.getMessage());
            throw e;
        } catch (Exception e) {
            markFailed(document, "文档处理失败");
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文档处理失败");
        }
    }

    @Transactional
    public KnowledgeDocumentVO importFromUrl(Long knowledgeBaseId, String url, String syncCron) {
        workspacePermissionService.requirePermission(PermissionCodes.KNOWLEDGE_UPLOAD);
        if (url == null || url.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请填写网页地址");
        }
        KnowledgeBase kb = knowledgeBaseApplicationService.requireKnowledgeBase(knowledgeBaseId);
        Long userId = WorkspaceContext.require().userId();
        String normalizedUrl = url.trim();
        byte[] bytes;
        try {
            bytes = java.net.URI.create(normalizedUrl).toURL().openStream().readAllBytes();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "抓取网页失败");
        }
        String originalName = normalizedUrl.replaceAll("[^a-zA-Z0-9._-]", "_") + ".html";
        if (originalName.length() > 120) {
            originalName = originalName.substring(originalName.length() - 120);
        }
        KnowledgeDocument document = new KnowledgeDocument();
        document.setWorkspaceId(kb.getWorkspaceId());
        document.setKnowledgeBaseId(kb.getId());
        document.setName(originalName);
        document.setFileName(originalName);
        document.setFileType("HTML");
        document.setMimeType("text/html");
        document.setFileSize((long) bytes.length);
        document.setStorageBucket(storageBucket);
        document.setChunkCount(0);
        document.setStatus("PARSING");
        document.setCreatedBy(userId);
        knowledgeDocumentRepository.save(document);
        String storageKey = "knowledge/" + kb.getId() + "/" + document.getId() + "/" + originalName;
        document.setStorageKey(storageKey);
        try {
            document.setMd5(md5(bytes));
            objectStorage.put(storageBucket, storageKey, new java.io.ByteArrayInputStream(bytes), bytes.length, "text/html");
            String text = documentTextExtractor.extract(bytes, originalName);
            processDocument(kb, document, text);
            knowledgeDocumentRepository.update(document);
            refreshKnowledgeBaseCounts(kb);
            return toVO(document);
        } catch (BusinessException e) {
            markFailed(document, e.getMessage());
            throw e;
        } catch (Exception e) {
            markFailed(document, "网页处理失败");
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "网页处理失败");
        }
    }

    @Transactional
    public KnowledgeDocumentVO retry(Long documentId) {
        workspacePermissionService.requirePermission(PermissionCodes.KNOWLEDGE_UPLOAD);
        KnowledgeDocument document = requireDocument(documentId);
        if (!"FAILED".equals(document.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅失败文档可重试");
        }
        if (document.getStorageBucket() == null || document.getStorageKey() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文档存储信息缺失，无法重试");
        }
        KnowledgeBase kb = knowledgeBaseApplicationService.requireKnowledgeBase(document.getKnowledgeBaseId());
        chunkIndexingService.deleteDocumentIndex(document.getId());
        knowledgeChunkRepository.deleteByDocument(document.getId());
        document.setErrorMessage(null);
        document.setChunkCount(0);
        document.setStatus("PARSING");
        knowledgeDocumentRepository.update(document);
        try (var input = objectStorage.get(document.getStorageBucket(), document.getStorageKey())) {
            byte[] bytes = input.readAllBytes();
            String text = documentTextExtractor.extract(bytes, document.getFileName());
            processDocument(kb, document, text);
            knowledgeDocumentRepository.update(document);
            refreshKnowledgeBaseCounts(kb);
            return toVO(document);
        } catch (BusinessException e) {
            markFailed(document, e.getMessage());
            throw e;
        } catch (Exception e) {
            markFailed(document, "文档重试失败");
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文档重试失败");
        }
    }

    @Transactional
    public void delete(Long documentId) {
        workspacePermissionService.requirePermission(PermissionCodes.KNOWLEDGE_DELETE);
        KnowledgeDocument document = requireDocument(documentId);
        KnowledgeBase kb = knowledgeBaseApplicationService.requireKnowledgeBase(document.getKnowledgeBaseId());
        chunkIndexingService.deleteDocumentIndex(document.getId());
        knowledgeChunkRepository.deleteByDocument(document.getId());
        if (document.getStorageBucket() != null && document.getStorageKey() != null) {
            try {
                objectStorage.delete(document.getStorageBucket(), document.getStorageKey());
            } catch (Exception ignored) {
                // ignore storage cleanup failure
            }
        }
        knowledgeDocumentRepository.delete(documentId);
        refreshKnowledgeBaseCounts(kb);
    }

    private void processDocument(KnowledgeBase kb, KnowledgeDocument document, String content) {
        document.setStatus("CHUNKING");
        knowledgeDocumentRepository.update(document);

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
        knowledgeChunkRepository.saveBatch(chunks);
        document.setStatus("EMBEDDING");
        knowledgeDocumentRepository.update(document);
        document.setStatus("INDEXING");
        knowledgeDocumentRepository.update(document);
        chunkIndexingService.indexChunks(kb, chunks);
        document.setChunkCount(chunks.size());
        document.setStatus("READY");
        notifyDocumentProcessed(document, true, null);
    }

    private void refreshKnowledgeBaseCounts(KnowledgeBase kb) {
        List<KnowledgeDocument> docs = knowledgeDocumentRepository.listByKnowledgeBase(kb.getId());
        kb.setDocumentCount(docs.size());
        kb.setChunkCount((long) knowledgeChunkRepository.countByKnowledgeBase(kb.getId()));
        knowledgeBaseRepository.update(kb);
    }

    private void markFailed(KnowledgeDocument document, String message) {
        document.setStatus("FAILED");
        document.setErrorMessage(message);
        knowledgeDocumentRepository.update(document);
        notifyDocumentProcessed(document, false, message);
    }

    private void notifyDocumentProcessed(KnowledgeDocument document, boolean success, String errorMessage) {
        Long userId = document.getCreatedBy();
        if (userId == null) {
            userId = WorkspaceContext.require().userId();
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

    KnowledgeDocument requireDocument(Long documentId) {
        KnowledgeDocument document = knowledgeDocumentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "文档不存在"));
        if (!workspaceId().equals(document.getWorkspaceId())) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "无权访问该文档");
        }
        return document;
    }

    private KnowledgeChunkVO toChunkVO(KnowledgeChunk chunk) {
        return new KnowledgeChunkVO(
                chunk.getId(),
                chunk.getChunkIndex(),
                chunk.getContent(),
                chunk.getTokenCount(),
                chunk.getStatus());
    }

    private KnowledgeDocumentVO toVO(KnowledgeDocument document) {
        return new KnowledgeDocumentVO(
                document.getId(),
                document.getKnowledgeBaseId(),
                document.getName(),
                document.getFileName(),
                document.getFileType(),
                document.getFileSize(),
                document.getChunkCount(),
                document.getStatus(),
                document.getErrorMessage(),
                document.getCreatedAt(),
                document.getUpdatedAt());
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

    private String resolveFileType(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "TXT";
        }
        return fileName.substring(dot + 1).toUpperCase(Locale.ROOT);
    }

    private String md5(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        return HexFormat.of().formatHex(digest.digest(bytes));
    }

    private Long workspaceId() {
        return WorkspaceContext.require().workspaceId();
    }
}
