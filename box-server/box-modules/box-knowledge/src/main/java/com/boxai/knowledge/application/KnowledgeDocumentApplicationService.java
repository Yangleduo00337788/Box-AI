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
import com.boxai.knowledge.support.KnowledgeDocumentProgress;
import com.boxai.common.security.FileSafetyPolicy;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.permission.WorkspacePermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
public class KnowledgeDocumentApplicationService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeDocumentApplicationService.class);

    private final KnowledgeBaseApplicationService knowledgeBaseApplicationService;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final ObjectStorage objectStorage;
    private final KnowledgeDocumentProcessingService knowledgeDocumentProcessingService;
    private final WorkspacePermissionService workspacePermissionService;
    private final TransactionTemplate transactionTemplate;

    public KnowledgeDocumentApplicationService(KnowledgeBaseApplicationService knowledgeBaseApplicationService,
                                               KnowledgeBaseRepository knowledgeBaseRepository,
                                               KnowledgeDocumentRepository knowledgeDocumentRepository,
                                               KnowledgeChunkRepository knowledgeChunkRepository,
                                               ObjectStorage objectStorage,
                                               KnowledgeDocumentProcessingService knowledgeDocumentProcessingService,
                                               WorkspacePermissionService workspacePermissionService,
                                               TransactionTemplate transactionTemplate) {
        this.knowledgeBaseApplicationService = knowledgeBaseApplicationService;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.knowledgeChunkRepository = knowledgeChunkRepository;
        this.objectStorage = objectStorage;
        this.knowledgeDocumentProcessingService = knowledgeDocumentProcessingService;
        this.workspacePermissionService = workspacePermissionService;
        this.transactionTemplate = transactionTemplate;
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
        String storageBucket = objectStorage.defaultBucket();
        document.setStorageBucket(storageBucket);
        document.setStorageBackend(objectStorage.activeBackend());
        document.setChunkCount(0);
        document.setStatus("UPLOADING");
        document.setProgress(KnowledgeDocumentProgress.UPLOADING);
        document.setCreatedBy(userId);
        transactionTemplate.executeWithoutResult(status -> knowledgeDocumentRepository.save(document));

        String storageKey = "knowledge/" + kb.getId() + "/" + document.getId() + "/" + originalName;
        document.setStorageKey(storageKey);
        try {
            document.setMd5(md5(bytes));
            objectStorage.put(storageBucket, storageKey, new java.io.ByteArrayInputStream(bytes), bytes.length, file.getContentType());
            transactionTemplate.executeWithoutResult(status -> {
                document.setStatus("QUEUED");
                document.setProgress(KnowledgeDocumentProgress.QUEUED);
                knowledgeDocumentRepository.update(document);
                refreshKnowledgeBaseCounts(kb);
            });
        } catch (BusinessException e) {
            transactionTemplate.executeWithoutResult(status -> markFailed(document, e.getMessage()));
            throw e;
        } catch (Exception e) {
            transactionTemplate.executeWithoutResult(status -> markFailed(document, "文档上传失败"));
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文档上传失败");
        }
        knowledgeDocumentProcessingService.enqueue(document.getId());
        return toVO(document);
    }

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
        String storageBucket = objectStorage.defaultBucket();
        document.setStorageBucket(storageBucket);
        document.setStorageBackend(objectStorage.activeBackend());
        document.setChunkCount(0);
        document.setStatus("UPLOADING");
        document.setProgress(KnowledgeDocumentProgress.UPLOADING);
        document.setCreatedBy(userId);
        transactionTemplate.executeWithoutResult(status -> knowledgeDocumentRepository.save(document));
        String storageKey = "knowledge/" + kb.getId() + "/" + document.getId() + "/" + originalName;
        document.setStorageKey(storageKey);
        try {
            document.setMd5(md5(bytes));
            objectStorage.put(storageBucket, storageKey, new java.io.ByteArrayInputStream(bytes), bytes.length, "text/html");
            transactionTemplate.executeWithoutResult(status -> {
                document.setStatus("QUEUED");
                document.setProgress(KnowledgeDocumentProgress.QUEUED);
                knowledgeDocumentRepository.update(document);
                refreshKnowledgeBaseCounts(kb);
            });
        } catch (BusinessException e) {
            transactionTemplate.executeWithoutResult(status -> markFailed(document, e.getMessage()));
            throw e;
        } catch (Exception e) {
            transactionTemplate.executeWithoutResult(status -> markFailed(document, "网页导入失败"));
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "网页导入失败");
        }
        knowledgeDocumentProcessingService.enqueue(document.getId());
        return toVO(document);
    }

    public KnowledgeDocumentVO retry(Long documentId) {
        workspacePermissionService.requirePermission(PermissionCodes.KNOWLEDGE_UPLOAD);
        KnowledgeDocument document = requireDocument(documentId);
        if ("READY".equals(document.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文档已处理完成，无需重试");
        }
        if (document.getStorageBucket() == null || document.getStorageKey() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文档存储信息缺失，无法重试");
        }
        knowledgeBaseApplicationService.requireKnowledgeBase(document.getKnowledgeBaseId());
        knowledgeDocumentProcessingService.deleteDocumentIndex(document.getId());
        transactionTemplate.executeWithoutResult(status -> {
            knowledgeChunkRepository.deleteByDocument(document.getId());
            document.setErrorMessage("");
            document.setChunkCount(0);
            document.setStatus("QUEUED");
            document.setProgress(KnowledgeDocumentProgress.QUEUED);
            knowledgeDocumentRepository.update(document);
        });
        knowledgeDocumentProcessingService.enqueue(document.getId());
        return toVO(document);
    }

    @Transactional
    public void delete(Long documentId) {
        workspacePermissionService.requirePermission(PermissionCodes.KNOWLEDGE_DELETE);
        KnowledgeDocument document = requireDocument(documentId);
        KnowledgeBase kb = knowledgeBaseApplicationService.requireKnowledgeBase(document.getKnowledgeBaseId());
        knowledgeDocumentProcessingService.deleteDocumentIndex(document.getId());
        knowledgeChunkRepository.deleteByDocument(document.getId());
        if (document.getStorageBucket() != null && document.getStorageKey() != null) {
            try {
                objectStorage.delete(document.getStorageBackend(), document.getStorageBucket(), document.getStorageKey());
            } catch (Exception ex) {
                log.warn(
                        "Failed to delete object storage for document {} (bucket={}, key={}): {}",
                        documentId,
                        document.getStorageBucket(),
                        document.getStorageKey(),
                        ex.getMessage());
            }
        }
        knowledgeDocumentRepository.delete(documentId);
        refreshKnowledgeBaseCounts(kb);
    }

    private void refreshKnowledgeBaseCounts(KnowledgeBase kb) {
        List<KnowledgeDocument> docs = knowledgeDocumentRepository.listByKnowledgeBase(kb.getId());
        kb.setDocumentCount(docs.size());
        kb.setChunkCount((long) knowledgeChunkRepository.countByKnowledgeBase(kb.getId()));
        knowledgeBaseRepository.update(kb);
    }

    private void markFailed(KnowledgeDocument document, String message) {
        document.setStatus("FAILED");
        if (document.getProgress() == null) {
            document.setProgress(0);
        }
        document.setErrorMessage(message);
        knowledgeDocumentRepository.update(document);
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
                document.getProgress(),
                document.getErrorMessage(),
                document.getCreatedAt(),
                document.getUpdatedAt());
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
