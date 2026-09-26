package com.boxai.knowledge.application;

import com.boxai.domain.knowledge.KnowledgeBase;
import com.boxai.domain.knowledge.KnowledgeBaseRepository;
import com.boxai.domain.knowledge.KnowledgeChunk;
import com.boxai.domain.knowledge.KnowledgeChunkRepository;
import com.boxai.domain.knowledge.KnowledgeDocument;
import com.boxai.domain.knowledge.KnowledgeDocumentRepository;
import com.boxai.domain.storage.ObjectStorage;
import com.boxai.knowledge.support.DocumentTextExtractor;
import com.boxai.security.notification.NotificationPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeDocumentProcessingServiceTest {

    @Mock
    private Executor knowledgeDocumentExecutor;
    @Mock
    private KnowledgeDocumentRepository knowledgeDocumentRepository;
    @Mock
    private KnowledgeBaseRepository knowledgeBaseRepository;
    @Mock
    private KnowledgeChunkRepository knowledgeChunkRepository;
    @Mock
    private ObjectStorage objectStorage;
    @Mock
    private KnowledgeChunkIndexingService chunkIndexingService;
    @Mock
    private DocumentTextExtractor documentTextExtractor;
    @Mock
    private KnowledgeOcrService knowledgeOcrService;
    @Mock
    private NotificationPublisher notificationPublisher;
    @Mock
    private TransactionTemplate transactionTemplate;

    @Test
    void processChunksPlainTextAndMarksReady() throws Exception {
        KnowledgeDocumentProcessingService service = newService();
        stubImmediateTransactions();

        KnowledgeDocument document = document(5L, "notes.txt", "PENDING");
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(2L);
        when(knowledgeDocumentRepository.findById(5L)).thenReturn(Optional.of(document));
        when(knowledgeBaseRepository.findById(2L)).thenReturn(Optional.of(kb));
        when(objectStorage.get(any(), eq("bucket"), eq("key")))
                .thenReturn(new ByteArrayInputStream("ignored".getBytes(StandardCharsets.UTF_8)));
        when(documentTextExtractor.extract(any(), eq("notes.txt"))).thenReturn("readable knowledge text");
        when(knowledgeOcrService.mayNeedOcr(eq("notes.txt"), any(), any())).thenReturn(false);
        when(knowledgeDocumentRepository.listByKnowledgeBase(2L)).thenReturn(List.of(document));
        when(knowledgeChunkRepository.countByKnowledgeBase(2L)).thenReturn(1);

        service.process(5L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<KnowledgeChunk>> captor = ArgumentCaptor.forClass(List.class);
        verify(knowledgeChunkRepository).saveBatch(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals("readable knowledge text", captor.getValue().get(0).getContent());
        verify(chunkIndexingService).indexChunks(eq(kb), anyList());
        assertEquals("READY", document.getStatus());
        verify(notificationPublisher).publish(eq(3L), eq(7L), eq("知识库文档处理完成"), any(), eq("KNOWLEDGE"), eq("/knowledge"));
    }

    @Test
    void processMarksFailedWhenExtractedTextIsEmpty() throws Exception {
        KnowledgeDocumentProcessingService service = newService();
        stubImmediateTransactions();

        KnowledgeDocument document = document(5L, "empty.txt", "PENDING");
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(2L);
        when(knowledgeDocumentRepository.findById(5L)).thenReturn(Optional.of(document));
        when(knowledgeBaseRepository.findById(2L)).thenReturn(Optional.of(kb));
        when(objectStorage.get(any(), eq("bucket"), eq("key")))
                .thenReturn(new ByteArrayInputStream(new byte[0]));
        when(documentTextExtractor.extract(any(), eq("empty.txt"))).thenReturn("   ");
        when(knowledgeOcrService.mayNeedOcr(eq("empty.txt"), any(), any())).thenReturn(false);

        service.process(5L);

        assertEquals("FAILED", document.getStatus());
        verify(chunkIndexingService, never()).indexChunks(any(), anyList());
        verify(notificationPublisher).publish(eq(3L), eq(7L), eq("知识库文档处理失败"), any(), eq("KNOWLEDGE"), eq("/knowledge"));
    }

    private KnowledgeDocumentProcessingService newService() {
        return new KnowledgeDocumentProcessingService(
                knowledgeDocumentExecutor,
                knowledgeDocumentRepository,
                knowledgeBaseRepository,
                knowledgeChunkRepository,
                objectStorage,
                chunkIndexingService,
                documentTextExtractor,
                knowledgeOcrService,
                notificationPublisher,
                transactionTemplate);
    }

    @SuppressWarnings("unchecked")
    private void stubImmediateTransactions() {
        doAnswer(invocation -> {
            Consumer<?> action = invocation.getArgument(0);
            action.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    private static KnowledgeDocument document(Long id, String fileName, String status) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(id);
        document.setWorkspaceId(7L);
        document.setKnowledgeBaseId(2L);
        document.setName("Doc");
        document.setFileName(fileName);
        document.setStorageBucket("bucket");
        document.setStorageKey("key");
        document.setStatus(status);
        document.setCreatedBy(3L);
        return document;
    }
}
