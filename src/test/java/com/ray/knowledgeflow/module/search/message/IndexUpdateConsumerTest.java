package com.ray.knowledgeflow.module.search.message;

import com.ray.knowledgeflow.module.ai.service.ChunkingService;
import com.ray.knowledgeflow.module.search.service.EmbeddingService;
import com.ray.knowledgeflow.module.search.service.ISearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import io.qdrant.client.grpc.Points.PointStruct;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IndexUpdateConsumerTest {

    @Mock
    private ISearchService searchService;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private ChunkingService chunkingService;

    private IndexUpdateConsumer indexUpdateConsumer;

    @BeforeEach
    void setUp() {
        indexUpdateConsumer = new IndexUpdateConsumer(searchService, embeddingService, chunkingService);
    }

    @Test
    void handleIndexUpdateTask_CreateContentItem_ShouldEmbedAndUpsert() {
        // Arrange
        IndexUpdateMessage message = new IndexUpdateMessage();
        message.setAction("CREATE");
        message.setDocumentType("CONTENT_ITEM");
        message.setDocumentId(123L);
        message.setUserId(7L);
        message.setTitle("Test Title");
        message.setContent("Test Content");
        message.setSummary("Test Summary");
        message.setTags("tag1,tag2");

        when(embeddingService.embed(anyString())).thenReturn(List.of(0.1f, 0.2f));

        // Act
        indexUpdateConsumer.handleIndexUpdateTask(message);

        // Assert
        verify(embeddingService).embed(contains("Test Title"));
        verify(embeddingService).createCollection("content");
        verify(searchService).syncContent(any(com.ray.knowledgeflow.module.content.entity.ContentItem.class));

        // 必须写入 userId payload，否则语义检索的用户过滤会失效
        ArgumentCaptor<List<PointStruct>> pointsCaptor = ArgumentCaptor.forClass(List.class);
        verify(embeddingService).upsert(eq("content"), pointsCaptor.capture());
        PointStruct point = pointsCaptor.getValue().get(0);
        assertEquals(7L, point.getPayloadMap().get("userId").getIntegerValue());

        // Verify ChunkingService called
        verify(chunkingService).initCollection();
        verify(chunkingService).indexContent(any(com.ray.knowledgeflow.module.content.entity.ContentItem.class));
    }

    @Test
    void handleIndexUpdateTask_WhenIndexingFails_ShouldPropagateForRabbitRetry() {
        IndexUpdateMessage message = new IndexUpdateMessage();
        message.setAction("CREATE");
        message.setDocumentType("CONTENT_ITEM");
        message.setDocumentId(123L);
        message.setUserId(7L);
        message.setTitle("Test");
        message.setContent("Content");
        when(embeddingService.embed(anyString())).thenThrow(new RuntimeException("embedding down"));

        assertThrows(IllegalStateException.class,
                () -> indexUpdateConsumer.handleIndexUpdateTask(message));
    }
}
