package com.ray.knowledgeflow.module.ai.service;

import com.ray.knowledgeflow.module.search.service.EmbeddingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * 语义切片器测试。边界判定为纯函数，可脱离嵌入服务验证。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SemanticChunkerTest {

    @Mock
    private EmbeddingService embeddingService;

    @InjectMocks
    private SemanticChunker chunker;

    // ==================== 句子切分 ====================

    @Test
    void splitSentences_ShouldSplitOnChineseAndEnglishPunctuation() {
        List<String> sentences = SemanticChunker.splitSentences(
                "这是第一句。这是第二句！第三句？This is fourth. Fifth one!");

        assertEquals(5, sentences.size());
        assertEquals("这是第一句。", sentences.get(0));
        assertEquals("第三句？", sentences.get(2));
    }

    @Test
    void splitSentences_ShouldNotBreakDecimalsOrDottedIdentifiers() {
        List<String> sentences = SemanticChunker.splitSentences("圆周率约为 3.14 而已。Node.js 很流行。");

        // 句点后无空白不算句子边界，否则技术笔记会被切得支离破碎
        assertEquals(2, sentences.size());
        assertTrue(sentences.get(0).contains("3.14"));
        assertTrue(sentences.get(1).contains("Node.js"));
    }

    @Test
    void splitSentences_ShouldTreatMarkdownHeadingAsOwnUnit() {
        List<String> sentences = SemanticChunker.splitSentences(
                "上一段的结尾。\n## 新章节标题\n新章节的内容。");

        // 标题不能和上一段落粘连，否则检索到的片段会跨越两个主题
        assertTrue(sentences.contains("## 新章节标题"));
        assertEquals("上一段的结尾。", sentences.get(0));
        assertEquals("## 新章节标题", sentences.get(1));
    }

    // ==================== 余弦距离 ====================

    @Test
    void cosineDistance_IdenticalVectors_ShouldBeZero() {
        List<Float> v = List.of(1f, 2f, 3f);
        assertEquals(0.0, SemanticChunker.cosineDistance(v, v), 1e-9);
    }

    @Test
    void cosineDistance_OrthogonalVectors_ShouldBeOne() {
        assertEquals(1.0,
                SemanticChunker.cosineDistance(List.of(1f, 0f), List.of(0f, 1f)), 1e-9);
    }

    // ==================== 分位数 ====================

    @Test
    void percentile_ShouldInterpolateBetweenValues() {
        List<Double> values = List.of(0.1, 0.2, 0.3, 0.4, 0.5);

        assertEquals(0.3, SemanticChunker.percentile(values, 50), 1e-9);
        assertEquals(0.5, SemanticChunker.percentile(values, 100), 1e-9);
        assertEquals(0.1, SemanticChunker.percentile(values, 0), 1e-9);
    }

    // ==================== 边界聚合 ====================

    @Test
    void assemble_ShouldBreakWhereDistanceExceedsThreshold() {
        List<String> sentences = List.of("A句。", "B句。", "C句。", "D句。");
        // 第 2、3 句之间语义跳变
        List<Double> distances = List.of(0.05, 0.9, 0.05);

        List<String> chunks = SemanticChunker.assemble(sentences, distances, 0.5, 1000);

        assertEquals(2, chunks.size());
        assertEquals("A句。B句。", chunks.get(0));
        assertEquals("C句。D句。", chunks.get(1));
    }

    @Test
    void assemble_ShouldBreakWhenExceedingMaxChars() {
        List<String> sentences = List.of("A".repeat(60), "B".repeat(60), "C".repeat(60));
        // 语义上完全连贯，不应产生语义断点
        List<Double> distances = List.of(0.0, 0.0);

        List<String> chunks = SemanticChunker.assemble(sentences, distances, 0.5, 100);

        // 但长度上限必须兜住，否则会超出嵌入模型输入限制
        assertEquals(3, chunks.size());
    }

    // ==================== 尺寸收尾 ====================

    @Test
    void enforceSizeLimits_ShouldMergeUndersizedChunks() {
        List<String> chunks = List.of("短", "也短", "这是一个足够长的切片内容用于测试合并行为");

        List<String> result = SemanticChunker.enforceSizeLimits(chunks, 10, 1000);

        // 过短的片段检索价值低，应合并
        assertTrue(result.size() < chunks.size());
    }

    @Test
    void enforceSizeLimits_ShouldHardSplitOversizedChunk() {
        List<String> result = SemanticChunker.enforceSizeLimits(List.of("X".repeat(250)), 10, 100);

        assertEquals(3, result.size());
        assertTrue(result.stream().allMatch(c -> c.length() <= 100));
    }

    // ==================== 端到端 ====================

    @Test
    void chunk_ShouldSplitAtSemanticShift() {
        chunker.configure(50, 1, 1000, 0);

        String text = "Java 的垃圾回收分为新生代和老年代。"
                + "新生代使用复制算法。"
                + "今天午饭吃了牛肉面。"
                + "面馆在公司楼下。";

        // 前两句谈 GC，后两句谈午饭：构造两组互相正交的向量
        when(embeddingService.embedAll(anyList())).thenReturn(List.of(
                List.of(1f, 0f),
                List.of(1f, 0f),
                List.of(0f, 1f),
                List.of(0f, 1f)));

        List<String> chunks = chunker.chunk(text);

        assertEquals(2, chunks.size());
        assertTrue(chunks.get(0).contains("垃圾回收"));
        assertTrue(chunks.get(1).contains("牛肉面"));
        // 主题不应混在同一个切片里
        assertFalse(chunks.get(0).contains("牛肉面"));
    }

    @Test
    void chunk_WhenEmbeddingFails_ShouldFallBackToFixedSize() {
        chunker.configure(85, 1, 100, 0);
        when(embeddingService.embedAll(anyList())).thenReturn(List.of());

        List<String> chunks = chunker.chunk("句子一。句子二。句子三。句子四。");

        // 嵌入不可用时不能让索引链路整个失败
        assertFalse(chunks.isEmpty());
    }

    @Test
    void chunk_EmptyInput_ShouldReturnEmpty() {
        assertTrue(chunker.chunk(null).isEmpty());
        assertTrue(chunker.chunk("   ").isEmpty());
    }
}
