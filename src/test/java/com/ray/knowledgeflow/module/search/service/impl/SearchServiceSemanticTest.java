package com.ray.knowledgeflow.module.search.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ray.knowledgeflow.module.content.dto.ContentItemDto;
import com.ray.knowledgeflow.module.content.entity.ContentItem;
import com.ray.knowledgeflow.module.content.mapper.ContentItemMapper;
import com.ray.knowledgeflow.module.note.mapper.NoteFolderMapper;
import com.ray.knowledgeflow.module.search.repository.ContentRepository;
import com.ray.knowledgeflow.module.search.repository.NoteRepository;
import com.ray.knowledgeflow.module.search.service.EmbeddingService;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.Futures;

@ExtendWith(MockitoExtension.class)
class SearchServiceSemanticTest {

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

        private SearchServiceImpl searchService;

        @BeforeEach
        void setUp() {
                searchService = new SearchServiceImpl(
                                noteRepository, contentRepository, noteFolderMapper,
                                elasticsearchOperations, embeddingService, qdrantClient, contentItemMapper);
        }

        @Test
        void searchContentSemantic_ShouldReturnResults() throws ExecutionException, InterruptedException {
                // Arrange
                String keyword = "AI";
                Long userId = 1L;
                List<Float> vector = List.of(0.1f, 0.2f);

                when(embeddingService.embed(keyword)).thenReturn(vector);

                Points.ScoredPoint scoredPoint = Points.ScoredPoint.newBuilder()
                                .setId(Points.PointId.newBuilder().setNum(100L).build())
                                .setScore(0.9f)
                                .build();

                when(qdrantClient.searchAsync(any(Points.SearchPoints.class)))
                                .thenReturn(Futures.immediateFuture(List.of(scoredPoint)));

                ContentItem item = new ContentItem();
                item.setId(100L);
                item.setTitle("AI Article");

                when(contentItemMapper.selectList(any(LambdaQueryWrapper.class)))
                                .thenReturn(List.of(item));

                // Act
                Page<ContentItemDto> result = searchService.searchContentSemantic(keyword, userId, 0, 10);

                // Assert
                assertFalse(result.isEmpty());
                assertEquals(1, result.getContent().size());
                assertEquals("AI Article", result.getContent().get(0).getTitle());
        }
}
