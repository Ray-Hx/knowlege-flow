package com.ray.knowledgeflow.module.ai.service;

import com.ray.knowledgeflow.module.search.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 语义切片器。
 *
 * <p>固定长度切分会把一段完整论述从中间截断，导致检索到的片段缺少上下文。
 * 这里改为按语义边界切分：为相邻句子求向量，在语义跳变处断开。
 *
 * <p>流程：句子切分 → 邻域拼接 → 批量求向量 → 相邻余弦距离 → 分位数阈值定边界 → 合并/拆分收尾。
 *
 * <p>阈值取分位数而非绝对值，因为不同文档的向量距离分布差异很大，
 * 绝对阈值在一篇文章里切得过碎、在另一篇里又完全不切。
 *
 * @author Ray
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SemanticChunker {

    private final EmbeddingService embeddingService;

    /** 断点分位数：取所有相邻距离的第 N 百分位作为切分阈值，越大切得越少 */
    @Value("${app.rag.chunk.breakpoint-percentile:85}")
    private int breakpointPercentile;

    /** 单个切片的字符数下限，低于此值会与相邻切片合并 */
    @Value("${app.rag.chunk.min-chars:80}")
    private int minChars;

    /** 单个切片的字符数上限，超出会强制拆分，避免超过嵌入模型输入限制 */
    @Value("${app.rag.chunk.max-chars:1000}")
    private int maxChars;

    /** 求向量时向前后各拼接的句子数，用于抑制单句噪声 */
    @Value("${app.rag.chunk.buffer-size:1}")
    private int bufferSize;

    /**
     * 句子边界。中文标点可直接断句；英文句点要求其后为空白，
     * 否则会把 3.14、Node.js 这类写法误切开。
     */
    private static final Pattern SENTENCE_END = Pattern.compile("(?<=[。！？；!?;])|(?<=\\.)(?=\\s)");

    /** Markdown 标题行，强制作为语义边界 */
    private static final Pattern HEADING = Pattern.compile("^\\s{0,3}#{1,6}\\s");

    /**
     * 按语义边界切分文本。
     *
     * <p>嵌入失败或句子过少时退化为定长切分，保证索引链路不中断。
     */
    public List<String> chunk(String text) {
        if (text == null || text.trim().isEmpty()) {
            return List.of();
        }

        List<String> sentences = splitSentences(text);
        if (sentences.isEmpty()) {
            return List.of();
        }
        // 句子太少时无从比较语义距离，直接按长度收尾
        if (sentences.size() < 3) {
            return enforceSizeLimits(sentences, minChars, maxChars);
        }

        List<List<Float>> embeddings = embeddingService.embedAll(buildWindows(sentences, bufferSize));
        if (embeddings.size() != sentences.size()) {
            log.warn("语义切片降级为定长切分: 句子数={}, 向量数={}", sentences.size(), embeddings.size());
            return fixedSizeFallback(text, maxChars);
        }

        List<Double> distances = adjacentDistances(embeddings);
        double threshold = percentile(distances, breakpointPercentile);

        List<String> chunks = assemble(sentences, distances, threshold, maxChars);
        return enforceSizeLimits(chunks, minChars, maxChars);
    }

    // ==================== 以下为纯函数，便于单独测试 ====================

    /**
     * 切分句子。Markdown 标题独立成句，保证标题不会和上一段落粘连。
     */
    static List<String> splitSentences(String text) {
        List<String> sentences = new ArrayList<>();
        for (String line : text.split("\n", -1)) {
            if (line.trim().isEmpty()) {
                continue;
            }
            // 标题行整行作为一个语义单元
            if (HEADING.matcher(line).find()) {
                sentences.add(line.trim());
                continue;
            }
            for (String part : SENTENCE_END.split(line)) {
                if (!part.trim().isEmpty()) {
                    sentences.add(part.trim());
                }
            }
        }
        return sentences;
    }

    /**
     * 把每个句子与前后邻居拼接后再求向量，降低单句噪声对边界判定的干扰。
     */
    static List<String> buildWindows(List<String> sentences, int bufferSize) {
        if (bufferSize <= 0) {
            return sentences;
        }
        List<String> windows = new ArrayList<>(sentences.size());
        for (int i = 0; i < sentences.size(); i++) {
            int from = Math.max(0, i - bufferSize);
            int to = Math.min(sentences.size(), i + bufferSize + 1);
            windows.add(String.join("", sentences.subList(from, to)));
        }
        return windows;
    }

    /**
     * 相邻向量的余弦距离，长度为 n-1
     */
    static List<Double> adjacentDistances(List<List<Float>> embeddings) {
        List<Double> distances = new ArrayList<>();
        for (int i = 0; i < embeddings.size() - 1; i++) {
            distances.add(cosineDistance(embeddings.get(i), embeddings.get(i + 1)));
        }
        return distances;
    }

    /**
     * 余弦距离 = 1 - 余弦相似度，取值 [0, 2]，越大表示语义差异越大
     */
    static double cosineDistance(List<Float> a, List<Float> b) {
        if (a == null || b == null || a.isEmpty() || a.size() != b.size()) {
            return 0.0;
        }
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.size(); i++) {
            double x = a.get(i);
            double y = b.get(i);
            dot += x * y;
            normA += x * x;
            normB += y * y;
        }
        if (normA == 0 || normB == 0) {
            return 0.0;
        }
        return 1.0 - dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 线性插值分位数
     */
    static double percentile(List<Double> values, int p) {
        if (values == null || values.isEmpty()) {
            return Double.MAX_VALUE;
        }
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        if (sorted.size() == 1) {
            return sorted.get(0);
        }
        double rank = (p / 100.0) * (sorted.size() - 1);
        int lower = (int) Math.floor(rank);
        int upper = (int) Math.ceil(rank);
        if (lower == upper) {
            return sorted.get(lower);
        }
        return sorted.get(lower) + (rank - lower) * (sorted.get(upper) - sorted.get(lower));
    }

    /**
     * 依据距离阈值把句子聚合为切片。距离超过阈值即认为语义发生跳变，在此断开。
     */
    static List<String> assemble(List<String> sentences, List<Double> distances,
            double threshold, int maxChars) {
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder(sentences.get(0));

        for (int i = 1; i < sentences.size(); i++) {
            String sentence = sentences.get(i);
            boolean semanticBreak = distances.get(i - 1) > threshold;
            boolean tooLong = current.length() + sentence.length() > maxChars;

            if (semanticBreak || tooLong) {
                chunks.add(current.toString());
                current = new StringBuilder(sentence);
            } else {
                current.append(sentence);
            }
        }
        chunks.add(current.toString());
        return chunks;
    }

    /**
     * 收尾：合并过短切片、硬拆过长切片。
     *
     * <p>语义边界不保证长度均匀，过短的片段检索价值低，过长的会超出嵌入模型限制。
     */
    static List<String> enforceSizeLimits(List<String> chunks, int minChars, int maxChars) {
        List<String> merged = new ArrayList<>();
        for (String chunk : chunks) {
            if (!merged.isEmpty() && merged.get(merged.size() - 1).length() < minChars
                    && merged.get(merged.size() - 1).length() + chunk.length() <= maxChars) {
                merged.set(merged.size() - 1, merged.get(merged.size() - 1) + chunk);
            } else {
                merged.add(chunk);
            }
        }

        List<String> result = new ArrayList<>();
        for (String chunk : merged) {
            if (chunk.length() <= maxChars) {
                result.add(chunk);
            } else {
                for (int i = 0; i < chunk.length(); i += maxChars) {
                    result.add(chunk.substring(i, Math.min(chunk.length(), i + maxChars)));
                }
            }
        }
        return result;
    }

    /**
     * 嵌入不可用时的兜底：纯按长度切，保证索引链路不中断
     */
    private static List<String> fixedSizeFallback(String text, int maxChars) {
        List<String> chunks = new ArrayList<>();
        String normalized = text.trim();
        for (int i = 0; i < normalized.length(); i += maxChars) {
            chunks.add(normalized.substring(i, Math.min(normalized.length(), i + maxChars)));
        }
        return chunks;
    }

    /** 供测试注入配置 */
    void configure(int breakpointPercentile, int minChars, int maxChars, int bufferSize) {
        this.breakpointPercentile = breakpointPercentile;
        this.minChars = minChars;
        this.maxChars = maxChars;
        this.bufferSize = bufferSize;
    }
}
