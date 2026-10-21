package com.ray.knowledgeflow.module.ai.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 重排器测试。解析逻辑为纯函数，重点覆盖模型返回不规范的各种情况。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LlmRerankerTest {

    @Mock
    private ChatLanguageModel chatModel;

    @InjectMocks
    private LlmReranker reranker;

    private static final List<String> CANDIDATES = List.of("片段0", "片段1", "片段2", "片段3", "片段4", "片段5");

    // ==================== 解析容错 ====================

    @Test
    void parseIndices_CleanJsonArray() {
        assertEquals(List.of(3, 0, 5), LlmReranker.parseIndices("[3,0,5]", 6, 3));
    }

    @Test
    void parseIndices_ShouldToleratePrefixAndSuffixText() {
        // 模型常常不听话，会加解释性文字
        assertEquals(List.of(2, 1),
                LlmReranker.parseIndices("根据相关性分析，结果如下：[2, 1] 希望对你有帮助", 6, 2));
    }

    @Test
    void parseIndices_ShouldDropOutOfRangeIndices() {
        // 模型可能凭空捏造越界下标，不过滤会直接数组越界
        assertEquals(List.of(1, 4), LlmReranker.parseIndices("[1,99,4,-3]", 6, 5));
    }

    @Test
    void parseIndices_ShouldDeduplicate() {
        assertEquals(List.of(2, 0), LlmReranker.parseIndices("[2,2,0,0]", 6, 5));
    }

    @Test
    void parseIndices_ShouldTruncateToTopK() {
        assertEquals(List.of(0, 1), LlmReranker.parseIndices("[0,1,2,3,4]", 6, 2));
    }

    @Test
    void parseIndices_MalformedResponse_ShouldReturnEmpty() {
        assertTrue(LlmReranker.parseIndices("我无法完成这个任务", 6, 3).isEmpty());
        assertTrue(LlmReranker.parseIndices("", 6, 3).isEmpty());
        assertTrue(LlmReranker.parseIndices(null, 6, 3).isEmpty());
    }

    @Test
    void parseIndices_EmptyArray_ShouldReturnEmpty() {
        assertTrue(LlmReranker.parseIndices("[]", 6, 3).isEmpty());
    }

    // ==================== prompt 构造 ====================

    @Test
    void buildPrompt_ShouldNumberCandidatesAndTruncateLongOnes() {
        String prompt = LlmReranker.buildPrompt("什么是GC", List.of("A".repeat(500), "短片段"), 2, 100);

        assertTrue(prompt.contains("什么是GC"));
        assertTrue(prompt.contains("[0]"));
        assertTrue(prompt.contains("[1]"));
        // 超长候选应被截断，避免撑爆 prompt
        assertTrue(prompt.contains("..."));
        assertTrue(prompt.length() < 800);
    }

    // ==================== 端到端行为 ====================

    @Test
    void rerank_ShouldReorderByModelOutput() {
        reranker.configure(true, 200);
        when(chatModel.generate(anyString())).thenReturn("[4,1,0]");

        assertEquals(List.of(4, 1, 0), reranker.rerank("问题", CANDIDATES, 3));
    }

    @Test
    void rerank_WhenModelFails_ShouldFallBackToVectorOrder() {
        reranker.configure(true, 200);
        when(chatModel.generate(anyString())).thenThrow(new RuntimeException("超时"));

        // 重排只做增强，失败不能让整个 RAG 链路挂掉
        assertEquals(List.of(0, 1, 2), reranker.rerank("问题", CANDIDATES, 3));
    }

    @Test
    void rerank_WhenGarbageResponse_ShouldFallBackToVectorOrder() {
        reranker.configure(true, 200);
        when(chatModel.generate(anyString())).thenReturn("抱歉，我不理解");

        assertEquals(List.of(0, 1, 2), reranker.rerank("问题", CANDIDATES, 3));
    }

    @Test
    void rerank_WhenDisabled_ShouldNotCallModel() {
        reranker.configure(false, 200);

        assertEquals(List.of(0, 1, 2), reranker.rerank("问题", CANDIDATES, 3));
        verify(chatModel, never()).generate(anyString());
    }

    @Test
    void rerank_WhenCandidatesFewerThanTopK_ShouldSkipModelCall() {
        reranker.configure(true, 200);

        // 候选还没 topK 多，重排没有意义，不该白白花一次调用
        assertEquals(List.of(0, 1), reranker.rerank("问题", List.of("a", "b"), 5));
        verify(chatModel, never()).generate(anyString());
    }

    @Test
    void rerank_EmptyCandidates_ShouldReturnEmpty() {
        reranker.configure(true, 200);
        assertTrue(reranker.rerank("问题", List.of(), 3).isEmpty());
    }
}
