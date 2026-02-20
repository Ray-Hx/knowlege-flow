package com.ray.knowledgeflow.module.ai.service;

import com.ray.knowledgeflow.module.content.entity.ContentItem;
import com.ray.knowledgeflow.module.search.service.EmbeddingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChunkingServiceTest {

    @Mock
    private EmbeddingService embeddingService;

    @InjectMocks
    private ChunkingService chunkingService;

    @Test
    void testIndexContent_SplitsAndEmbeds() {
        // Arrange
        String longText = "This is a long text that needs to be chunked. ".repeat(50);
        ContentItem item = new ContentItem();
        item.setId(1L);
        item.setTitle("Test Title");
        item.setContentText(longText);

        when(embeddingService.embed(anyString())).thenReturn(List.of(0.1f, 0.2f, 0.3f));

        // Act
        chunkingService.indexContent(item);

        // Assert
        // We expect one batch upsert call containing multiple points
        org.mockito.ArgumentCaptor<List> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(embeddingService, times(1)).upsert(eq("content_chunks"), captor.capture());

        List points = captor.getValue();
        // Total length approx 2300 chars. Chunk size 500. Expected ~5 chunks.
        org.junit.jupiter.api.Assertions.assertTrue(points.size() >= 4,
                "Should have at least 4 chunks, but got " + points.size());

        // Verify metadata
        io.qdrant.client.grpc.Points.PointStruct firstPoint = (io.qdrant.client.grpc.Points.PointStruct) points.get(0);
        java.util.Map<String, io.qdrant.client.grpc.JsonWithInt.Value> payloadMap = firstPoint.getPayloadMap();
        org.junit.jupiter.api.Assertions.assertTrue(payloadMap.containsKey("title"));
        org.junit.jupiter.api.Assertions.assertEquals("Test Title", payloadMap.get("title").getStringValue());

        // ContentItem in test setup didn't set url/author, so they should be empty
        // strings as per service logic
        org.junit.jupiter.api.Assertions.assertTrue(payloadMap.containsKey("url"));
        org.junit.jupiter.api.Assertions.assertEquals("", payloadMap.get("url").getStringValue());
    }

    @Test
    void testIndexContent_EmptyContent() {
        ContentItem item = new ContentItem();
        item.setContentText("");

        chunkingService.indexContent(item);

        verify(embeddingService, never()).embed(anyString());
        verify(embeddingService, never()).upsert(anyString(), anyList());
    }
}
