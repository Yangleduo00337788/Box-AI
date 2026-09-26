package com.boxai.knowledge.application;

import com.boxai.common.constant.PermissionCodes;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.knowledge.KnowledgeBase;
import com.boxai.domain.knowledge.KnowledgeBaseRepository;
import com.boxai.domain.knowledge.KnowledgeChunkRepository;
import com.boxai.domain.knowledge.KnowledgeDocument;
import com.boxai.domain.knowledge.KnowledgeDocumentRepository;
import com.boxai.domain.storage.ObjectStorage;
import com.boxai.knowledge.support.KnowledgeDocumentProgress;
import com.boxai.security.context.WorkspaceContext;
import com.boxai.security.permission.WorkspacePermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeDocumentApplicationServiceTest {

    @Mock
    private KnowledgeBaseApplicationService knowledgeBaseApplicationService;
    @Mock
    private KnowledgeBaseRepository knowledgeBaseRepository;
    @Mock
    private KnowledgeDocumentRepository knowledgeDocumentRepository;
    @Mock
    private KnowledgeChunkRepository knowledgeChunkRepository;
    @Mock
    private ObjectStorage objectStorage;
    @Mock
    private KnowledgeDocumentProcessingService knowledgeDocumentProcessingService;
    @Mock
    private WorkspacePermissionService workspacePermissionService;
    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private KnowledgeDocumentApplicationService service;

    @AfterEach
    void tearDown() {
        WorkspaceContext.clear();
    }

    @Test
    void uploadRejectsEmptyFile() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        MultipartFile empty = new MockMultipartFile("file", "note.txt", "text/plain", new byte[0]);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.upload(4L, empty));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        verify(knowledgeDocumentRepository, never()).save(any());
    }

    @Test
    void uploadStoresFileAndEnqueuesProcessing() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        KnowledgeBase kb = knowledgeBase();
        when(knowledgeBaseApplicationService.requireKnowledgeBase(4L)).thenReturn(kb);
        when(objectStorage.defaultBucket()).thenReturn("box");
        when(objectStorage.activeBackend()).thenReturn("MINIO");
        runTransactions();
        doAnswer(invocation -> {
            KnowledgeDocument document = invocation.getArgument(0);
            document.setId(15L);
            return document;
        }).when(knowledgeDocumentRepository).save(any(KnowledgeDocument.class));
        when(knowledgeDocumentRepository.listByKnowledgeBase(4L)).thenReturn(List.of(new KnowledgeDocument()));
        when(knowledgeChunkRepository.countByKnowledgeBase(4L)).thenReturn(0);

        var vo = service.upload(4L, new MockMultipartFile("file", "note.txt", "text/plain", "hello world".getBytes()));

        verify(workspacePermissionService).requirePermission(PermissionCodes.KNOWLEDGE_UPLOAD);
        verify(objectStorage).put(eq("box"), eq("knowledge/4/15/note.txt"), any(), eq(11L), eq("text/plain"));
        verify(knowledgeDocumentProcessingService).enqueue(15L);
        assertEquals("QUEUED", vo.status());
        assertEquals(KnowledgeDocumentProgress.QUEUED, vo.progress());
        assertEquals("TXT", vo.fileType());
        assertEquals(15L, vo.id());
    }

    @Test
    void importFromUrlRejectsBlank() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.importFromUrl(4L, "  ", null));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    }

    @Test
    void retryRejectsReadyDocument() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        KnowledgeDocument document = document(15L, 7L);
        document.setStatus("READY");
        when(knowledgeDocumentRepository.findById(15L)).thenReturn(Optional.of(document));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.retry(15L));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        verify(knowledgeDocumentProcessingService, never()).enqueue(anyLong());
    }

    @Test
    void retryRejectsMissingStorage() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        KnowledgeDocument document = document(15L, 7L);
        document.setStatus("FAILED");
        when(knowledgeDocumentRepository.findById(15L)).thenReturn(Optional.of(document));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.retry(15L));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    }

    @Test
    void deleteRejectsOtherWorkspace() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        when(knowledgeDocumentRepository.findById(15L)).thenReturn(Optional.of(document(15L, 99L)));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete(15L));
        assertEquals(ErrorCode.WORKSPACE_ACCESS_DENIED, ex.getCode());
        verify(knowledgeDocumentRepository, never()).delete(anyLong());
    }

    @Test
    void deleteRemovesIndexChunksAndObject() {
        WorkspaceContext.set(new WorkspaceContext(7L, 3L, 1L, "MEMBER"));
        KnowledgeDocument document = document(15L, 7L);
        document.setStorageBackend("MINIO");
        document.setStorageBucket("box");
        document.setStorageKey("knowledge/4/15/note.txt");
        when(knowledgeDocumentRepository.findById(15L)).thenReturn(Optional.of(document));
        KnowledgeBase kb = knowledgeBase();
        when(knowledgeBaseApplicationService.requireKnowledgeBase(4L)).thenReturn(kb);
        when(knowledgeDocumentRepository.listByKnowledgeBase(4L)).thenReturn(List.of());
        when(knowledgeChunkRepository.countByKnowledgeBase(4L)).thenReturn(0);

        service.delete(15L);

        verify(workspacePermissionService).requirePermission(PermissionCodes.KNOWLEDGE_DELETE);
        verify(knowledgeDocumentProcessingService).deleteDocumentIndex(15L);
        verify(knowledgeChunkRepository).deleteByDocument(15L);
        verify(objectStorage).delete("MINIO", "box", "knowledge/4/15/note.txt");
        verify(knowledgeDocumentRepository).delete(15L);
        verify(knowledgeBaseRepository).update(kb);
        assertEquals(0, kb.getDocumentCount());
    }

    private void runTransactions() {
        doAnswer(invocation -> {
            invocation.<Consumer<TransactionStatus>>getArgument(0).accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    private static KnowledgeBase knowledgeBase() {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(4L);
        kb.setWorkspaceId(7L);
        return kb;
    }

    private static KnowledgeDocument document(Long id, Long workspaceId) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(id);
        document.setWorkspaceId(workspaceId);
        document.setKnowledgeBaseId(4L);
        return document;
    }
}
