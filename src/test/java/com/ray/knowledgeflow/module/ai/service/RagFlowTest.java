package com.ray.knowledgeflow.module.ai.service;

import com.ray.knowledgeflow.module.content.entity.ContentItem;
import com.ray.knowledgeflow.module.search.message.IndexUpdateConsumer;
import com.ray.knowledgeflow.module.search.message.IndexUpdateMessage;
import com.ray.knowledgeflow.module.search.service.EmbeddingService;
import com.ray.knowledgeflow.module.search.service.impl.SearchServiceImpl;
import com.ray.knowledgeflow.module.search.service.impl.SearchServiceImpl;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.timeout;

@SpringBootTest
@ActiveProfiles("test")
@org.springframework.test.context.TestPropertySource(properties = {
                "qdrant.host=localhost",
                "qdrant.port=6334",
                "qdrant.api-key=dummy",
                "langchain4j.zhipu.api-key=dummy",
                "knowledge-flow.ai.zhipuai.api-key=dummy",
                "spring.neo4j.uri=bolt://localhost:7687",
                "spring.neo4j.authentication.username=neo4j",
                "spring.neo4j.authentication.password=password",
                "spring.rabbitmq.host=localhost",
                "spring.rabbitmq.port=5672",
                "spring.rabbitmq.listener.simple.auto-startup=false"
})
public class RagFlowTest {

        @Autowired
        private IndexUpdateConsumer indexUpdateConsumer;

        @Autowired
        private SearchServiceImpl searchService;

        @MockBean
        private EmbeddingService embeddingService;

        @MockBean
        private ChatLanguageModel chatLanguageModel; // Mock AI to avoid API calls

        @MockBean
        private com.ray.knowledgeflow.module.content.mapper.ContentItemMapper contentItemMapper;

        @MockBean
        private io.qdrant.client.QdrantClient qdrantClient;

        @MockBean
        private com.ray.knowledgeflow.module.search.repository.NoteRepository noteRepository;

        @MockBean
        private com.ray.knowledgeflow.module.search.repository.ContentRepository contentRepository;

        @Test
        void testIngestAndSearchFlow() throws Exception {
                // --- 1. Ingest Flow ---
                // Mock EmbeddingService for ingestion
                when(embeddingService.embed(anyString())).thenReturn(List.of(0.1f, 0.2f));

                IndexUpdateMessage message = new IndexUpdateMessage();
                message.setDocumentId(100L);
                message.setDocumentType("CONTENT_ITEM");
                message.setAction("CREATE");
                message.setTitle("RAG Test Document");
                message.setContent("This is a test content for RAG flow verification.");

                // Action: Trigger ingestion
                indexUpdateConsumer.handleIndexUpdateTask(message);

                // Verify:
                // 1. embeddingService.embed called
                verify(embeddingService, org.mockito.Mockito.atLeastOnce()).embed(anyString());
                // 2. embeddingService.upsert called twice (once for whole doc "content", once
                // for chunks "content_chunks")
                verify(embeddingService, org.mockito.Mockito.atLeast(2)).upsert(anyString(), any());

                // --- 2. Search Flow ---
                // Mock Qdrant search response
                io.qdrant.client.grpc.Points.ScoredPoint scoredPoint = io.qdrant.client.grpc.Points.ScoredPoint
                                .newBuilder()
                                .setId(io.qdrant.client.grpc.Points.PointId.newBuilder().setNum(100L).build())
                                .setScore(0.95f)
                                .putPayload("sourceId",
                                                io.qdrant.client.grpc.JsonWithInt.Value.newBuilder()
                                                                .setIntegerValue(100).build())
                                .build();

                // Mock searchAsync to return our scored point
                when(qdrantClient.searchAsync(any(io.qdrant.client.grpc.Points.SearchPoints.class)))
                                .thenReturn(com.google.common.util.concurrent.Futures
                                                .immediateFuture(List.of(scoredPoint)));

                // Mock Database Layer (ContentItemMapper)
                ContentItem mockItem = new ContentItem();
                mockItem.setId(100L);
                mockItem.setTitle("RAG Test Document");
                when(contentItemMapper.selectList(any())).thenReturn(List.of(mockItem));

                // Action: Search
                org.springframework.data.domain.Page<com.ray.knowledgeflow.module.content.dto.ContentItemDto> results = searchService
                                .searchContentSemantic("test query", null, 0, 10);

                // Verify
                org.junit.jupiter.api.Assertions.assertFalse(results.isEmpty());
                org.junit.jupiter.api.Assertions.assertEquals(100L, results.getContent().get(0).getId());
        }
}
