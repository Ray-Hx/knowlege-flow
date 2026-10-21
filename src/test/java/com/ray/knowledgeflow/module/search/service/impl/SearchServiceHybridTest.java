package com.ray.knowledgeflow.module.search.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ray.knowledgeflow.module.content.dto.ContentItemDto;
import com.ray.knowledgeflow.module.content.entity.ContentItem;
import com.ray.knowledgeflow.module.content.mapper.ContentItemMapper;
import com.ray.knowledgeflow.module.note.mapper.NoteFolderMapper;
import com.ray.knowledgeflow.module.search.entity.ContentDocument;
import com.ray.knowledgeflow.module.search.repository.ContentRepository;
import com.ray.knowledgeflow.module.search.repository.NoteRepository;
import com.ray.knowledgeflow.module.search.service.EmbeddingService;
import com.ray.knowledgeflow.module.ai.service.Reranker;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Query;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.mockito.ArgumentCaptor;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.common.util.concurrent.Futures;

@ExtendWith(MockitoExtension.class)
class SearchServiceHybridTest {

        @Mock
        private NoteRepository noteRepository;
        @Mock
        private ContentRepository contentRepository;
        @Mock
        private NoteFolderMapper noteFolderMapper;
        @Mock
        private ElasticsearchOperations elasticsearchOperations;
        @Mock
        private EmbeddingService embeddingService;
        @Mock
        private QdrantClient qdrantClient;
        @Mock
        private ContentItemMapper contentItemMapper;
        @Mock
        private Reranker reranker;
        @Mock
        private java.util.concurrent.Executor searchTaskExecutor;

        @Mock
        private com.ray.knowledgeflow.module.note.mapper.NoteEntryMapper noteMapper;

        @InjectMocks
        private SearchServiceImpl searchService;

        @BeforeEach
        void setUp() {
                lenient().doAnswer(invocation -> {
                        ((Runnable) invocation.getArgument(0)).run();
                        return null;
                }).when(searchTaskExecutor).execute(any(Runnable.class));
        }

        @Test
        void searchContentHybrid_ShouldRerankRrfCandidatesBeforePaging()
                        throws ExecutionException, InterruptedException {
                // Arrange
                String keyword = "test";
                Long userId = 1L;

                // Mock ES results: [1, 2]
                ContentDocument doc1 = new ContentDocument();
                doc1.setId(1L);
                ContentDocument doc2 = new ContentDocument();
                doc2.setId(2L);
                SearchHit<ContentDocument> hit1 = new org.springframework.data.elasticsearch.core.SearchHit<>(
                                null, "", "", 1.0f, null, null, null, null, null, null, doc1);
                SearchHit<ContentDocument> hit2 = new org.springframework.data.elasticsearch.core.SearchHit<>(
                                null, "", "", 0.9f, null, null, null, null, null, null, doc2);

                // Use Mockito to mock SearchHits interface to avoid constructor mismatch issues
                SearchHits<ContentDocument> esHits = mock(SearchHits.class);
                when(esHits.stream()).thenAnswer(i -> List.of(hit1, hit2).stream());

                lenient().doReturn(esHits).when(elasticsearchOperations).search(any(Query.class), any());

                // Mock Qdrant results: [2, 3]
                when(embeddingService.embed(keyword)).thenReturn(List.of(0.1f));
                Points.ScoredPoint p2 = Points.ScoredPoint.newBuilder().setId(Points.PointId.newBuilder().setNum(2L))
                                .build();
                Points.ScoredPoint p3 = Points.ScoredPoint.newBuilder().setId(Points.PointId.newBuilder().setNum(3L))
                                .build();
                when(qdrantClient.searchAsync(any(Points.SearchPoints.class)))
                                .thenReturn(Futures.immediateFuture(List.of(p2, p3)));

                // Mock DB lookup
                ContentItem item1 = new ContentItem();
                item1.setId(1L);
                item1.setTitle("Item 1");
                item1.setSummary("Summary 1");
                ContentItem item2 = new ContentItem();
                item2.setId(2L);
                item2.setTitle("Item 2");
                ContentItem item3 = new ContentItem();
                item3.setId(3L);
                item3.setTitle("Item 3");
                item3.setContentText("Content 3");
                when(contentItemMapper.selectList(any(LambdaQueryWrapper.class)))
                                .thenReturn(List.of(item1, item2, item3));

                // RRF 顺序为 [2, 1, 3]，reranker 选择原下标 2、0，
                // 未返回的下标 1 应在最后按 RRF 顺序补齐。
                when(reranker.rerank(eq(keyword), any(), anyInt())).thenReturn(List.of(2, 0));

                // Act
                Page<ContentItemDto> result = searchService.searchContentHybrid(keyword, userId, 0, 10);

                // Assert
                assertNotNull(result);
                assertEquals(3, result.getTotalElements());
                assertEquals(3L, result.getContent().get(0).getId());
                assertEquals(2L, result.getContent().get(1).getId());
                assertEquals(1L, result.getContent().get(2).getId());

                @SuppressWarnings("unchecked")
                ArgumentCaptor<List<String>> documentsCaptor = ArgumentCaptor.forClass(List.class);
                verify(reranker).rerank(eq(keyword), documentsCaptor.capture(), eq(2));
                assertEquals(List.of(
                                "标题：Item 2",
                                "标题：Item 1\n摘要：Summary 1",
                                "标题：Item 3\n正文：Content 3"), documentsCaptor.getValue());
        }
}
