package com.boxai.knowledge.application;

import com.boxai.ai.ModelRuntimeConfig;
import com.boxai.ai.RerankModelGateway;
import com.boxai.ai.RerankScore;
import com.boxai.common.constant.PermissionCodes;
import com.boxai.domain.knowledge.KnowledgeBase;
import com.boxai.domain.knowledge.KnowledgeBaseRepository;
import com.boxai.domain.knowledge.KnowledgeChunk;
import com.boxai.domain.knowledge.KnowledgeChunkRepository;
import com.boxai.domain.knowledge.KnowledgeChunkSearchIndex;
import com.boxai.knowledge.api.KnowledgeSearchRequest;
import com.boxai.security.permission.WorkspacePermissionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeSearchServiceTest {

    @Mock
    private KnowledgeBaseApplicationService knowledgeBaseApplicationService;
    @Mock
    private KnowledgeBaseRepository knowledgeBaseRepository;
    @Mock
    private KnowledgeChunkRepository knowledgeChunkRepository;
    @Mock
    private KnowledgeChunkSearchIndex searchIndex;
    @Mock
    private KnowledgeChunkIndexingService chunkIndexingService;
    @Mock
    private RerankModelGateway rerankModelGateway;
    @Mock
    private WorkspacePermissionService workspacePermissionService;

    @InjectMocks
    private KnowledgeSearchService service;

    @Test
    void searchRequiresReadPermissionAndFusesHybridRanks() {
        KnowledgeBase kb = kb(null);
        when(knowledgeBaseApplicationService.requireKnowledgeBase(4L)).thenReturn(kb);
        when(knowledgeBaseRepository.findById(4L)).thenReturn(Optional.of(kb));
        when(chunkIndexingService.embedQuery(kb, "退款")).thenReturn(new float[] {0.1f});
        when(searchIndex.searchByVector(eq(4L), any(), eq(2))).thenReturn(List.of(1L, 2L));
        when(searchIndex.searchByKeyword(eq(4L), eq("退款"), anyList(), eq(2))).thenReturn(List.of(2L, 3L));
        when(knowledgeChunkRepository.findByIds(anyList())).thenReturn(List.of(
                chunk(1L, "a"), chunk(2L, "refund"), chunk(3L, "c")));

        var hits = service.search(4L, new KnowledgeSearchRequest("退款", 2));

        verify(workspacePermissionService).requirePermission(PermissionCodes.KNOWLEDGE_READ);
        assertEquals(2, hits.size());
        assertEquals(2L, hits.get(0).chunkId());
        verify(rerankModelGateway, never()).rerank(any(), any(), any(), anyInt());
    }

    @Test
    void searchHitsReturnsEmptyForBlankQuery() {
        assertTrue(service.searchHits(4L, "  ", 5, "HYBRID", null, false).isEmpty());
        verify(searchIndex, never()).searchByKeyword(any(), any(), any(), anyInt());
        verify(knowledgeBaseApplicationService, never()).requireKnowledgeBase(any());
    }

    @Test
    void searchHitsFallsBackToDatabaseWhenIndexEmpty() {
        KnowledgeBase kb = kb(null);
        when(knowledgeBaseApplicationService.requireKnowledgeBase(4L)).thenReturn(kb);
        when(knowledgeBaseRepository.findById(4L)).thenReturn(Optional.of(kb));
        when(chunkIndexingService.embedQuery(kb, "hello")).thenReturn(new float[0]);
        when(searchIndex.searchByKeyword(eq(4L), eq("hello"), anyList(), eq(3))).thenReturn(List.of());
        KnowledgeChunk chunk = chunk(9L, "say hello world");
        when(knowledgeChunkRepository.searchByKeyword(eq(4L), eq("hello"), eq(9))).thenReturn(List.of(chunk));

        var hits = service.searchHits(4L, "hello", 3, "HYBRID", null, false);

        assertEquals(1, hits.size());
        assertEquals(9L, hits.get(0).chunkId());
        assertTrue(hits.get(0).score() > 0);
    }

    @Test
    void searchHitsAppliesRerankAndThreshold() {
        KnowledgeBase kb = kb(12L);
        when(knowledgeBaseApplicationService.requireKnowledgeBase(4L)).thenReturn(kb);
        when(knowledgeBaseRepository.findById(4L)).thenReturn(Optional.of(kb));
        when(chunkIndexingService.embedQuery(kb, "q")).thenReturn(new float[0]);
        when(searchIndex.searchByKeyword(eq(4L), eq("q"), anyList(), eq(20))).thenReturn(List.of(1L, 2L));
        when(knowledgeChunkRepository.findByIds(anyList())).thenReturn(List.of(chunk(1L, "one"), chunk(2L, "two")));
        when(chunkIndexingService.resolveRerankConfig(kb)).thenReturn(new ModelRuntimeConfig("http://r", "k", "rerank"));
        when(rerankModelGateway.rerank(any(), eq("q"), anyList(), eq(20)))
                .thenReturn(List.of(new RerankScore(1, 0.9), new RerankScore(0, 0.2)));

        var hits = service.searchHits(4L, "q", 5, "HYBRID", 0.5, true);

        assertEquals(1, hits.size());
        assertEquals(2L, hits.get(0).chunkId());
        assertEquals(0.9, hits.get(0).score());
    }

    private static KnowledgeBase kb(Long rerankModelId) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(4L);
        kb.setWorkspaceId(7L);
        kb.setRerankModelId(rerankModelId);
        return kb;
    }

    private static KnowledgeChunk chunk(Long id, String content) {
        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setId(id);
        chunk.setDocumentId(20L);
        chunk.setChunkIndex(0);
        chunk.setContent(content);
        return chunk;
    }
}
