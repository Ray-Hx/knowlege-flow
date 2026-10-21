package com.ray.knowledgeflow.module.ai.service;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.ray.knowledgeflow.module.search.service.EmbeddingService;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RAG 检索器测试：覆盖「召回 - 精排」两段式流程与各类降级。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RagRetrieverTest {

    @Mock
    private EmbeddingService embeddingService;
    @Mock
    private QdrantClient qdrantClient;
    @Mock
    private Reranker reranker;

    @InjectMocks
    private RagRetriever retriever;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(retriever, "candidateSize", 20);
        ReflectionTestUtils.setField(retriever, "topK", 3);
        ReflectionTestUtils.setField(retriever, "scoreThreshold", 0.5f);
        ReflectionTestUtils.setField(retriever, "timeoutMs", 100L);
    }

    private Points.ScoredPoint point(long sourceId, String title, String content, float score) {
        return Points.ScoredPoint.newBuilder()
                .setId(Points.PointId.newBuilder().setNum(sourceId))
                .putPayload("content", JsonWithInt.Value.newBuilder().setStringValue(content).build())
                .putPayload("title", JsonWithInt.Value.newBuilder().setStringValue(title).build())
                .putPayload("sourceId", JsonWithInt.Value.newBuilder().setIntegerValue(sourceId).build())
                .putPayload("sourceType", JsonWithInt.Value.newBuilder().setStringValue("NOTE").build())
                .setScore(score)
                .build();
    }

    private void givenCandidates(List<Points.ScoredPoint> points) {
        when(embeddingService.embed(anyString())).thenReturn(List.of(0.1f, 0.2f));
        when(qdrantClient.searchAsync(any(Points.SearchPoints.class)))
                .thenReturn(Futures.immediateFuture(points));
    }

    @Test
    void retrieve_ShouldRequestMoreCandidatesThanTopK() {
        givenCandidates(List.of(point(1L, "T1", "C1", 0.9f)));
        when(reranker.rerank(anyString(), anyList(), anyInt())).thenReturn(List.of(0));

        retriever.retrieve("问题", 1L, (id, type) -> "NOTE");

        // 召回阶段必须多取，否则精排无从选择
        ArgumentCaptor<Points.SearchPoints> captor = ArgumentCaptor.forClass(Points.SearchPoints.class);
        verify(qdrantClient).searchAsync(captor.capture());
        assertEquals(20, captor.getValue().getLimit());
    }

    @Test
    void retrieve_ShouldFilterByUserId() {
        givenCandidates(List.of(point(1L, "T1", "C1", 0.9f)));
        when(reranker.rerank(anyString(), anyList(), anyInt())).thenReturn(List.of(0));

        retriever.retrieve("问题", 42L, (id, type) -> "NOTE");

        ArgumentCaptor<Points.SearchPoints> captor = ArgumentCaptor.forClass(Points.SearchPoints.class);
        verify(qdrantClient).searchAsync(captor.capture());
        List<Points.Condition> must = captor.getValue().getFilter().getMustList();
        assertTrue(must.stream().anyMatch(c -> "userId".equals(c.getField().getKey())
                && c.getField().getMatch().getInteger() == 42L),
                "向量检索缺少 userId 过滤，会检索到他人笔记");
    }

    @Test
    void retrieve_ShouldAssembleInRerankedOrder() {
        givenCandidates(List.of(
                point(1L, "标题A", "内容A", 0.9f),
                point(2L, "标题B", "内容B", 0.8f),
                point(3L, "标题C", "内容C", 0.7f)));
        // 重排把第 3 个提到最前
        when(reranker.rerank(anyString(), anyList(), anyInt())).thenReturn(List.of(2, 0));

        RagRetriever.RagContext context = retriever.retrieve("问题", 1L, (id, type) -> "NOTE");

        String prompt = context.getPrompt();
        assertTrue(prompt.indexOf("内容C") < prompt.indexOf("内容A"),
                "prompt 中片段顺序应遵循重排结果");
        // 未入选的候选不应出现
        assertFalse(prompt.contains("内容B"));
        assertEquals(2, context.getReferences().size());
    }

    @Test
    void retrieve_ShouldDeduplicateReferencesBySourceId() {
        // 同一篇笔记的两个切片都被召回
        givenCandidates(List.of(
                point(7L, "同一篇", "切片一", 0.9f),
                point(7L, "同一篇", "切片二", 0.8f)));
        when(reranker.rerank(anyString(), anyList(), anyInt())).thenReturn(List.of(0, 1));

        RagRetriever.RagContext context = retriever.retrieve("问题", 1L, (id, type) -> "NOTE");

        // 引用列表按来源去重，避免前端显示重复条目
        assertEquals(1, context.getReferences().size());
        // 但两个切片的正文都应进入上下文
        assertTrue(context.getPrompt().contains("切片一"));
        assertTrue(context.getPrompt().contains("切片二"));
    }

    @Test
    void retrieve_NoCandidates_ShouldFallBackToRawQuery() {
        givenCandidates(new ArrayList<>());

        RagRetriever.RagContext context = retriever.retrieve("原始问题", 1L, (id, type) -> "NOTE");

        assertEquals("原始问题", context.getPrompt());
        assertTrue(context.getReferences().isEmpty());
    }

    @Test
    void retrieve_WhenEmbeddingEmpty_ShouldFallBackToRawQuery() {
        when(embeddingService.embed(anyString())).thenReturn(List.of());

        RagRetriever.RagContext context = retriever.retrieve("原始问题", 1L, (id, type) -> "NOTE");

        assertEquals("原始问题", context.getPrompt());
    }

    @Test
    void retrieve_WhenSearchThrows_ShouldFallBackInsteadOfPropagating() {
        when(embeddingService.embed(anyString())).thenReturn(List.of(0.1f));
        when(qdrantClient.searchAsync(any(Points.SearchPoints.class)))
                .thenThrow(new RuntimeException("Qdrant 不可用"));

        // 检索故障不应让整个问答失败
        RagRetriever.RagContext context = retriever.retrieve("原始问题", 1L, (id, type) -> "NOTE");

        assertEquals("原始问题", context.getPrompt());
    }

    @Test
    void retrieve_WhenSearchTimesOut_ShouldFallBackInsteadOfBlocking() {
        when(embeddingService.embed(anyString())).thenReturn(List.of(0.1f));
        SettableFuture<List<Points.ScoredPoint>> pending = SettableFuture.create();
        when(qdrantClient.searchAsync(any(Points.SearchPoints.class))).thenReturn(pending);
        ReflectionTestUtils.setField(retriever, "timeoutMs", 1L);

        RagRetriever.RagContext context = retriever.retrieve("原始问题", 1L, (id, type) -> "NOTE");

        assertEquals("原始问题", context.getPrompt());
        assertTrue(context.getReferences().isEmpty());
    }
}
