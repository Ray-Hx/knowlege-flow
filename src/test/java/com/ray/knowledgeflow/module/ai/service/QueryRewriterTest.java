package com.ray.knowledgeflow.module.ai.service;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 查询改写测试：覆盖首轮短路、输出清洗，以及各类不可用输出的降级。
 *
 * <p>改写是增强而非必需，任何异常路径都必须退回原始问题，不能阻断问答。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QueryRewriterTest {

    @Mock
    private ChatLanguageModel chatModel;

    @InjectMocks
    private QueryRewriter rewriter;

    private static final List<ChatMessage> HISTORY = List.of(
            UserMessage.from("什么是 RRF 融合？"),
            dev.langchain4j.data.message.AiMessage.from("RRF 是倒数排名融合算法……"));

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(rewriter, "enabled", true);
        ReflectionTestUtils.setField(rewriter, "historySize", 4);
        ReflectionTestUtils.setField(rewriter, "historyChars", 200);
        ReflectionTestUtils.setField(rewriter, "maxResultChars", 200);
    }

    @Test
    void rewrite_ShouldReturnModelOutput() {
        when(chatModel.generate(anyString())).thenReturn("RRF 融合和 BM25 相比有什么优势？");

        assertEquals("RRF 融合和 BM25 相比有什么优势？",
                rewriter.rewrite("它和 BM25 比呢？", HISTORY));
    }

    @Test
    void rewrite_OnFirstTurn_ShouldSkipModelCall() {
        // 首轮没有上下文可依赖，省下一次模型调用
        assertEquals("什么是 RRF？", rewriter.rewrite("什么是 RRF？", List.of()));
        verify(chatModel, never()).generate(anyString());
    }

    @Test
    void rewrite_WhenDisabled_ShouldSkipModelCall() {
        ReflectionTestUtils.setField(rewriter, "enabled", false);

        assertEquals("它和 BM25 比呢？", rewriter.rewrite("它和 BM25 比呢？", HISTORY));
        verify(chatModel, never()).generate(anyString());
    }

    @Test
    void rewrite_WhenModelFails_ShouldFallBackToRawQuery() {
        when(chatModel.generate(anyString())).thenThrow(new RuntimeException("超时"));

        assertEquals("它和 BM25 比呢？", rewriter.rewrite("它和 BM25 比呢？", HISTORY));
    }

    @Test
    void rewrite_WhenOutputUnusable_ShouldFallBackToRawQuery() {
        when(chatModel.generate(anyString())).thenReturn("   ");

        assertEquals("它和 BM25 比呢？", rewriter.rewrite("它和 BM25 比呢？", HISTORY));
    }

    @Test
    void sanitize_ShouldStripQuotesAndTrailingExplanation() {
        // 模型不守规矩时的两种典型输出
        assertEquals("RRF 和 BM25 的对比", rewriter.sanitize("“RRF 和 BM25 的对比”"));
        assertEquals("RRF 和 BM25 的对比",
                rewriter.sanitize("RRF 和 BM25 的对比\n\n说明：我把\"它\"替换成了 RRF。"));
    }

    @Test
    void sanitize_ShouldRejectBlankAndOverlongOutput() {
        org.junit.jupiter.api.Assertions.assertNull(rewriter.sanitize(""));
        org.junit.jupiter.api.Assertions.assertNull(rewriter.sanitize(null));
        // 超长说明模型没按要求只输出问题，判为不可用
        org.junit.jupiter.api.Assertions.assertNull(rewriter.sanitize("好".repeat(201)));
    }

    @Test
    void buildPrompt_ShouldCarryHistoryAndLatestQuestion() {
        String prompt = rewriter.buildPrompt("它和 BM25 比呢？", HISTORY);

        assertTrue(prompt.contains("用户：什么是 RRF 融合？"));
        assertTrue(prompt.contains("助手：RRF 是倒数排名融合算法……"));
        assertTrue(prompt.contains("最新问题：它和 BM25 比呢？"));
    }

    @Test
    void buildPrompt_ShouldAbbreviateLongHistoryEntries() {
        ReflectionTestUtils.setField(rewriter, "historyChars", 5);

        String prompt = rewriter.buildPrompt("追问", List.of(
                UserMessage.from("问题"),
                dev.langchain4j.data.message.AiMessage.from("0123456789")));

        assertTrue(prompt.contains("助手：01234..."));
    }
}
