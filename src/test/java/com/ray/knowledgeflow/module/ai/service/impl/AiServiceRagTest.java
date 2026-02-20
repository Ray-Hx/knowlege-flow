package com.ray.knowledgeflow.module.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.Futures;
import com.ray.knowledgeflow.module.ai.dto.SendMessageDto;
import com.ray.knowledgeflow.module.ai.entity.AiConversation;
import com.ray.knowledgeflow.module.ai.entity.AiMessage;
import com.ray.knowledgeflow.module.ai.mapper.AiConversationMapper;
import com.ray.knowledgeflow.module.ai.mapper.AiMessageMapper;
import com.ray.knowledgeflow.module.search.service.EmbeddingService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiServiceRagTest {

    @Mock
    private AiConversationMapper conversationMapper;
    @Mock
    private AiMessageMapper messageMapper;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private ChatLanguageModel chatModel;
    @Mock
    private StreamingChatLanguageModel streamingChatModel;
    @Mock
    private EmbeddingService embeddingService;
    @Mock
    private QdrantClient qdrantClient;
    @Mock
    private com.ray.knowledgeflow.module.ai.mapper.AiAnalysisMapper analysisMapper;

    private AiServiceImpl aiService;

    @BeforeEach
    void setUp() {
        aiService = new AiServiceImpl(conversationMapper, messageMapper, objectMapper, chatModel,
                streamingChatModel, embeddingService, qdrantClient, analysisMapper);
    }

    @Test
    void sendMessage_ShouldUseRag_WhenFlagIsTrue() throws Exception {
        // Arrange
        Long userId = 123L;
        Long conversationId = 1L;
        SendMessageDto dto = new SendMessageDto();
        dto.setContent("test query");
        dto.setUseRag(true);

        // Mock Conversation
        AiConversation conversation = new AiConversation();
        conversation.setId(conversationId);
        conversation.setUserId(userId);
        conversation.setTitle("Test");
        when(conversationMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(conversation);

        // Mock Embedding
        when(embeddingService.embed("test query")).thenReturn(List.of(0.1f, 0.2f));

        // Mock Qdrant
        Points.ScoredPoint point = Points.ScoredPoint.newBuilder()
                .setId(Points.PointId.newBuilder().setNum(1L))
                .putPayload("content", JsonWithInt.Value.newBuilder().setStringValue("Rag Content").build())
                .putPayload("title", JsonWithInt.Value.newBuilder().setStringValue("Rag Title").build())
                .putPayload("sourceId", JsonWithInt.Value.newBuilder().setIntegerValue(100).build())
                .setScore(0.9f)
                .build();

        when(qdrantClient.searchAsync(any(Points.SearchPoints.class)))
                .thenReturn(Futures.immediateFuture(List.of(point)));

        // Mock LLM
        when(chatModel.generate(anyString())).thenReturn("AI Answer");

        // Act
        aiService.sendMessage(userId, conversationId, dto);

        // Assert
        // Verify Embedding called
        verify(embeddingService).embed("test query");
        // Verify Qdrant called
        verify(qdrantClient).searchAsync(any(Points.SearchPoints.class));
        // Verify LLM called with context
        verify(chatModel).generate(contains("Rag Content"));

        // Verify messages saved
        verify(messageMapper, times(2)).insert(any(AiMessage.class));
    }
}
