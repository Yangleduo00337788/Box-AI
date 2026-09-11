package com.boxai.knowledge.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.knowledge.KnowledgeBase;
import com.boxai.domain.knowledge.KnowledgeBaseRepository;
import com.boxai.domain.knowledge.KnowledgeChunk;
import com.boxai.domain.knowledge.KnowledgeChunkRepository;
import com.boxai.domain.knowledge.KnowledgeDocument;
import com.boxai.domain.knowledge.KnowledgeDocumentRepository;
import com.boxai.domain.storage.ObjectStorage;
import com.boxai.knowledge.api.KnowledgeDocumentVO;
import com.boxai.security.context.WorkspaceContext;
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

    public KnowledgeDocumentApplicationService(KnowledgeBaseApplicationService knowledgeBaseApplicationService,
                                               KnowledgeBaseRepository knowledgeBaseRepository,
                                               KnowledgeDocumentRepository knowledgeDocumentRepository,
                                               KnowledgeChunkRepository knowledgeChunkRepository,
                                               ObjectStorage objectStorage,
                                               KnowledgeChunkIndexingService chunkIndexingService,
                                               @Value("${box.minio.bucket:box}") String storageBucket) {
        this.knowledgeBaseApplicationService = knowledgeBaseApplicationService;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.knowledgeChunkRepository = knowledgeChunkRepository;
        this.objectStorage = objectStorage;
        this.chunkIndexingService = chunkIndexingService;
        this.storageBucket = storageBucket;
    }

    public List<KnowledgeDocumentVO> list(Long knowledgeBaseId) {
        knowledgeBaseApplicationService.requireKnowledgeBase(knowledgeBaseId);
        return knowledgeDocumentRepository.listByKnowledgeBase(knowledgeBaseId).stream().map(this::toVO).toList();
    }

    public KnowledgeDocumentVO detail(Long documentId) {
        return toVO(requireDocument(documentId));
    }

    @Transactional
    public KnowledgeDocumentVO upload(Long knowledgeBaseId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请上传文件");
        }
        KnowledgeBase kb = knowledgeBaseApplicationService.requireKnowledgeBase(knowledgeBaseId);
        Long userId = WorkspaceContext.require().userId();
        String originalName = file.getOriginalFilename() == null ? "document.txt" : file.getOriginalFilename();
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
            byte[] bytes = file.getBytes();
            document.setMd5(md5(bytes));
            objectStorage.put(storageBucket, storageKey, file.getInputStream(), file.getSize(), file.getContentType());
            processDocument(kb, document, new String(bytes, StandardCharsets.UTF_8));
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
    public void delete(Long documentId) {
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
        chunkIndexingService.indexChunks(kb, chunks);
        document.setChunkCount(chunks.size());
        document.setStatus("READY");
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
    }

    KnowledgeDocument requireDocument(Long documentId) {
        KnowledgeDocument document = knowledgeDocumentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "文档不存在"));
        if (!workspaceId().equals(document.getWorkspaceId())) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "无权访问该文档");
        }
        return document;
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
