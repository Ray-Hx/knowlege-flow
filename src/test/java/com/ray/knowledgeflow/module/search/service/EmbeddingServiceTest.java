package com.ray.knowledgeflow.module.search.service;

import com.google.common.util.concurrent.Futures;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private QdrantClient qdrantClient;

    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        embeddingService = new EmbeddingService(embeddingModel, qdrantClient);
    }

    @Test
    void testEmbed_ShouldReturnVector() {
        // Arrange
        String text = "Hello World";
        // Create a mock float array of size 2048 (simulating embedding-3)
        float[] vector = new float[2048];
        vector[0] = 0.1f;
        vector[1] = 0.2f;

        Embedding embedding = new Embedding(vector);
        Response<Embedding> response = Response.from(embedding);

        when(embeddingModel.embed(text)).thenReturn(response);

        // Act
        List<Float> result = embeddingService.embed(text);

        // Assert
        assertFalse(result.isEmpty());
        assertEquals(2048, result.size());
        assertEquals(0.1f, result.get(0));
    }

    @Test
    void testCreateCollection_WhenNotExists_ShouldCreate() {
        // Arrange
        String collectionName = "test_collection";

        when(qdrantClient.listCollectionsAsync())
                .thenReturn(Futures.immediateFuture(List.of()));

        when(qdrantClient.createCollectionAsync(anyString(), any(Collections.VectorParams.class)))
                .thenReturn(Futures.immediateFuture(Collections.CollectionOperationResponse.getDefaultInstance()));

        // Act
        embeddingService.createCollection(collectionName);

        // Assert
        verify(qdrantClient, times(1)).createCollectionAsync(eq(collectionName), any(Collections.VectorParams.class));
    }
}
