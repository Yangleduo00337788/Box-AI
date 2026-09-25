package com.boxai.knowledge.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.common.security.FileSafetyPolicy;
import com.boxai.domain.knowledge.KnowledgeBase;
import com.boxai.domain.knowledge.KnowledgeBaseRepository;
import com.boxai.domain.knowledge.KnowledgeDocument;
import com.boxai.domain.knowledge.KnowledgeDocumentRepository;
import com.boxai.domain.storage.ObjectStorage;
import com.boxai.knowledge.support.KnowledgeDocumentProgress;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * 从官方插件 catalog 资源导入文档到工作空间知识库（安装插件时调用，不走 C 端上传权限）。
 */
@Service
public class KnowledgePluginBundleImportService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final ObjectStorage objectStorage;
    private final KnowledgeDocumentProcessingService knowledgeDocumentProcessingService;
    private final TransactionTemplate transactionTemplate;

    public KnowledgePluginBundleImportService(KnowledgeBaseRepository knowledgeBaseRepository,
                                              KnowledgeDocumentRepository knowledgeDocumentRepository,
                                              ObjectStorage objectStorage,
                                              KnowledgeDocumentProcessingService knowledgeDocumentProcessingService,
                                              TransactionTemplate transactionTemplate) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.objectStorage = objectStorage;
        this.knowledgeDocumentProcessingService = knowledgeDocumentProcessingService;
        this.transactionTemplate = transactionTemplate;
    }

    public void importFromCatalogAsset(Long knowledgeBaseId,
                                       Long workspaceId,
                                       Long userId,
                                       String catalogStorageKey,
                                       String fileName) {
        KnowledgeBase kb = knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在"));
        if (!workspaceId.equals(kb.getWorkspaceId())) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "知识库不属于当前工作空间");
        }
        byte[] bytes = readCatalogAsset(catalogStorageKey);
        String safeName = FileSafetyPolicy.sanitizeFileName(fileName);
        FileSafetyPolicy.validate(safeName, null, bytes.length, bytes);

        KnowledgeDocument document = new KnowledgeDocument();
        document.setWorkspaceId(workspaceId);
        document.setKnowledgeBaseId(kb.getId());
        document.setName(safeName);
        document.setFileName(safeName);
        document.setFileType(resolveFileType(safeName));
        document.setFileSize((long) bytes.length);
        String bucket = objectStorage.defaultBucket();
        document.setStorageBucket(bucket);
        document.setStorageBackend(objectStorage.activeBackend());
        document.setChunkCount(0);
        document.setStatus("UPLOADING");
        document.setProgress(KnowledgeDocumentProgress.UPLOADING);
        document.setCreatedBy(userId);
        transactionTemplate.executeWithoutResult(status -> knowledgeDocumentRepository.save(document));

        String storageKey = "knowledge/" + kb.getId() + "/" + document.getId() + "/" + safeName;
        document.setStorageKey(storageKey);
        try {
            document.setMd5(md5(bytes));
            objectStorage.put(bucket, storageKey, new java.io.ByteArrayInputStream(bytes), bytes.length, guessMime(safeName));
            transactionTemplate.executeWithoutResult(status -> {
                document.setStatus("QUEUED");
                document.setProgress(KnowledgeDocumentProgress.QUEUED);
                knowledgeDocumentRepository.update(document);
                kb.setDocumentCount((kb.getDocumentCount() == null ? 0 : kb.getDocumentCount()) + 1);
                knowledgeBaseRepository.update(kb);
            });
            knowledgeDocumentProcessingService.enqueue(document.getId());
        } catch (Exception ex) {
            transactionTemplate.executeWithoutResult(status -> {
                document.setStatus("FAILED");
                document.setProgress(KnowledgeDocumentProgress.FAILED);
                knowledgeDocumentRepository.update(document);
            });
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导入插件文档失败");
        }
    }

    private byte[] readCatalogAsset(String catalogStorageKey) {
        if (catalogStorageKey == null || !catalogStorageKey.startsWith("plugin-catalog/")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的插件资源");
        }
        try (InputStream in = objectStorage.get(objectStorage.defaultBucket(), catalogStorageKey)) {
            return in.readAllBytes();
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "插件资源不存在");
        }
    }

    private static String resolveFileType(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) {
            return "txt";
        }
        return fileName.substring(dot + 1).toLowerCase();
    }

    private static String guessMime(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (lower.endsWith(".md")) {
            return "text/markdown";
        }
        return "application/octet-stream";
    }

    private static String md5(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(bytes));
        } catch (Exception ex) {
            return null;
        }
    }
}
