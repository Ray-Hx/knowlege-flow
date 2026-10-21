package com.ray.knowledgeflow.module.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ray.knowledgeflow.module.ai.dto.NoteReferenceDto;
import com.ray.knowledgeflow.module.ai.dto.SendMessageDto;
import com.ray.knowledgeflow.module.ai.entity.AiConversation;
import com.ray.knowledgeflow.module.ai.entity.AiMessage;
import com.ray.knowledgeflow.module.ai.mapper.AiConversationMapper;
import com.ray.knowledgeflow.module.ai.mapper.AiMessageMapper;
import com.ray.knowledgeflow.module.ai.service.ConversationMemory;
import com.ray.knowledgeflow.module.ai.service.QueryRewriter;
import com.ray.knowledgeflow.module.ai.service.RagRetriever;
import com.ray.knowledgeflow.module.search.service.EmbeddingService;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import io.qdrant.client.QdrantClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AI 服务的 RAG 编排测试。
 *
 * <p>检索与重排的具体行为由 {@code RagRetrieverTest} 和 {@code LlmRerankerTest} 覆盖，
 * 这里只验证编排：开关生效、检索结果被拼进 prompt、历史窗口被回灌、消息落库。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
    private com.ray.knowledgeflow.module.content.mapper.ContentItemMapper contentItemMapper;
    @Mock
    private com.ray.knowledgeflow.module.ai.mapper.AiAnalysisMapper analysisMapper;
    @Mock
    private RagRetriever ragRetriever;
    @Mock
    private ConversationMemory conversationMemory;
    @Mock
    private QueryRewriter queryRewriter;
    @Mock
    private com.ray.knowledgeflow.module.note.mapper.NoteEntryMapper noteEntryMapper;
    @Mock
    private com.ray.knowledgeflow.module.content.mapper.ContentSourceMapper contentSourceMapper;
    @Mock
    private java.util.concurrent.Executor aiTaskExecutor;

    private AiServiceImpl aiService;

    @BeforeEach
    void setUp() {
        aiService = new AiServiceImpl(conversationMapper, messageMapper, objectMapper, chatModel,
                streamingChatModel, embeddingService, qdrantClient, contentItemMapper, analysisMapper,
                ragRetriever, conversationMemory, queryRewriter, noteEntryMapper, contentSourceMapper,
                aiTaskExecutor);

        when(chatModel.generate(anyList()))
                .thenReturn(Response.from(dev.langchain4j.data.message.AiMessage.from("AI Answer")));
        // 默认无历史（首轮），需要多轮场景的用例自行覆盖
        when(conversationMemory.load(any())).thenReturn(List.of());
        when(queryRewriter.rewrite(anyString(), anyList())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void givenConversation(Long userId, Long conversationId) {
        AiConversation conversation = new AiConversation();
        conversation.setId(conversationId);
        conversation.setUserId(userId);
        conversation.setTitle("Test");
        when(conversationMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(conversation);
    }

    @SuppressWarnings("unchecked")
    private List<ChatMessage> captureSentMessages() {
        ArgumentCaptor<List<ChatMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(chatModel).generate(captor.capture());
        return captor.getValue();
    }

    @Test
    void sendMessage_ShouldFeedRetrievedContextIntoPrompt() {
        Long userId = 123L;
        Long conversationId = 1L;
        givenConversation(userId, conversationId);

        SendMessageDto dto = new SendMessageDto();
        dto.setContent("test query");
        dto.setUseRag(true);

        when(ragRetriever.retrieve(eq("test query"), eq("test query"), eq(userId), any()))
                .thenReturn(RagRetriever.RagContext.of(
                        "参考资料：Rag Content\n问题：test query",
                        List.of(new NoteReferenceDto(100L, "Rag Title", "Rag Content", 0.9, "NOTE", "NOTE"))));

        aiService.sendMessage(userId, conversationId, dto);

        // 检索结果必须进入 prompt，否则 RAG 形同虚设
        List<ChatMessage> sent = captureSentMessages();
        assertEquals(1, sent.size(), "首轮无历史，只应发送当前提问");
        assertTrue(sent.get(0).text().contains("Rag Content"));
        // 用户消息与 AI 回复各存一条
        verify(messageMapper, times(2)).insert(any(AiMessage.class));
    }

    @Test
    void sendMessage_WhenRagDisabled_ShouldNotRetrieve() {
        Long userId = 123L;
        Long conversationId = 1L;
        givenConversation(userId, conversationId);

        SendMessageDto dto = new SendMessageDto();
        dto.setContent("plain question");
        dto.setUseRag(false);

        aiService.sendMessage(userId, conversationId, dto);

        verify(ragRetriever, never()).retrieve(anyString(), anyString(), any(), any());
        // 关闭 RAG 时直接使用原始问题
        List<ChatMessage> sent = captureSentMessages();
        assertEquals("plain question", sent.get(0).text());
    }

    @Test
    void sendMessage_ShouldReplayHistoryBeforeCurrentTurn() {
        Long userId = 123L;
        Long conversationId = 1L;
        givenConversation(userId, conversationId);

        List<ChatMessage> history = List.of(
                UserMessage.from("什么是 RRF 融合？"),
                dev.langchain4j.data.message.AiMessage.from("RRF 是倒数排名融合……"));
        when(conversationMemory.load(conversationId)).thenReturn(history);

        SendMessageDto dto = new SendMessageDto();
        dto.setContent("它和 BM25 比呢？");
        dto.setUseRag(false);

        aiService.sendMessage(userId, conversationId, dto);

        // 历史必须在当前提问之前送进模型，否则模型无从解析"它"
        List<ChatMessage> sent = captureSentMessages();
        assertEquals(3, sent.size());
        assertEquals("什么是 RRF 融合？", sent.get(0).text());
        assertEquals("RRF 是倒数排名融合……", sent.get(1).text());
        assertEquals("它和 BM25 比呢？", sent.get(2).text());
    }

    @Test
    void sendMessage_ShouldRetrieveWithRewrittenQueryButPromptWithOriginal() {
        Long userId = 123L;
        Long conversationId = 1L;
        givenConversation(userId, conversationId);

        List<ChatMessage> history = List.of(
                UserMessage.from("什么是 RRF 融合？"),
                dev.langchain4j.data.message.AiMessage.from("RRF 是倒数排名融合……"));
        when(conversationMemory.load(conversationId)).thenReturn(history);
        when(queryRewriter.rewrite("它和 BM25 比呢？", history))
                .thenReturn("RRF 融合和 BM25 相比有什么优势？");
        when(ragRetriever.retrieve(anyString(), anyString(), any(), any()))
                .thenReturn(RagRetriever.RagContext.of("参考资料：……\n问题：它和 BM25 比呢？", List.of()));

        SendMessageDto dto = new SendMessageDto();
        dto.setContent("它和 BM25 比呢？");
        dto.setUseRag(true);

        aiService.sendMessage(userId, conversationId, dto);

        // 召回用改写后的自包含查询，写进 prompt 的仍是用户原话
        verify(ragRetriever).retrieve(
                eq("RRF 融合和 BM25 相比有什么优势？"), eq("它和 BM25 比呢？"), eq(userId), any());
    }

    @Test
    void sendMessage_ShouldLoadHistoryBeforePersistingCurrentQuestion() {
        Long userId = 123L;
        Long conversationId = 1L;
        givenConversation(userId, conversationId);

        SendMessageDto dto = new SendMessageDto();
        dto.setContent("当前提问");
        dto.setUseRag(false);

        aiService.sendMessage(userId, conversationId, dto);

        // 顺序颠倒会让当前提问混进历史窗口，模型看到同一个问题两次。
        // 用 times(2) 卡死本轮的两次落库（提问 + 回复）都在 load 之后
        var order = org.mockito.Mockito.inOrder(conversationMemory, messageMapper);
        order.verify(conversationMemory).load(conversationId);
        order.verify(messageMapper, times(2)).insert(any(AiMessage.class));
    }

    @Test
    void generateDeepAnalysis_ShouldRejectForeignNoteBeforeReadingCache() {
        when(noteEntryMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        assertThrows(com.ray.knowledgeflow.common.exception.BusinessException.class,
                () -> aiService.generateDeepAnalysis(7L, "private content", "NOTE", 99L));

        verify(analysisMapper, never()).selectOne(any(LambdaQueryWrapper.class));
        verify(chatModel, never()).generate(anyString());
    }
}
