package com.ray.knowledgeflow.module.ai.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于大模型的列表式重排器（listwise rerank）。
 *
 * <p>向量检索按语义相似度召回，但"相似"不等于"能回答问题"——
 * 高相似度片段可能只是用词接近。重排阶段让模型直接判断每个候选
 * 对当前问题的可用性，把精排交给理解能力更强的模型。
 *
 * <p>采用列表式而非逐条打分：所有候选放进同一个 prompt 一次调用完成，
 * 逐条打分要 N 次调用，延迟和成本都不可接受。
 *
 * <p>任何异常或解析失败都退回向量检索的原始顺序，重排只做增强、不做阻断。
 *
 * @author Ray
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.rag.rerank.provider", havingValue = "llm")
public class LlmReranker implements Reranker {

    private final ChatLanguageModel chatModel;

    /** 关闭后直接返回原始顺序，便于对比重排效果 */
    @Value("${app.rag.rerank.enabled:true}")
    private boolean enabled;

    /** 送入重排的候选片段最大字符数，过长会撑爆 prompt */
    @Value("${app.rag.rerank.snippet-chars:200}")
    private int snippetChars;

    /**
     * 匹配模型返回的 JSON 数组，容忍前后的解释性文字。
     * 字符类需包含负号：模型偶尔会返回负数下标，若因此整体匹配失败，
     * 会把本可用的其余下标一起丢掉。
     */
    private static final Pattern INDEX_ARRAY = Pattern.compile("\\[([-\\d,\\s]*)\\]");

    /**
     * 重排候选片段，返回相关性从高到低的前 topK 个。
     *
     * @param query      用户问题
     * @param candidates 候选片段文本，顺序为向量检索的原始顺序
     * @param topK       需要保留的数量
     * @return 重排后的下标列表，元素为 candidates 的索引
     */
    @Override
    public List<Integer> rerank(String query, List<String> candidates, int topK) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        // 候选数不超过需求量时无需重排
        if (!enabled || candidates.size() <= topK) {
            return Reranker.naturalOrder(Math.min(candidates.size(), topK));
        }

        try {
            String response = chatModel.generate(buildPrompt(query, candidates, topK, snippetChars));
            List<Integer> ranked = parseIndices(response, candidates.size(), topK);

            if (ranked.isEmpty()) {
                log.warn("重排结果解析为空，退回向量顺序. response={}", abbreviate(response));
                return Reranker.naturalOrder(topK);
            }
            return ranked;
        } catch (Exception e) {
            log.warn("重排调用失败，退回向量顺序: {}", e.getMessage());
            return Reranker.naturalOrder(topK);
        }
    }

    // ==================== 纯函数，便于单独测试 ====================

    /**
     * 构造列表式重排 prompt。要求模型只输出下标数组，降低解析难度。
     */
    static String buildPrompt(String query, List<String> candidates, int topK, int snippetChars) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是检索结果重排器。根据用户问题，从候选片段中挑出最能回答该问题的片段。\n\n");
        sb.append("用户问题：").append(query).append("\n\n");
        sb.append("候选片段：\n");

        for (int i = 0; i < candidates.size(); i++) {
            String text = candidates.get(i);
            if (text != null && text.length() > snippetChars) {
                text = text.substring(0, snippetChars) + "...";
            }
            sb.append("[").append(i).append("] ").append(text).append("\n");
        }

        sb.append("\n请按相关性从高到低，返回最相关的 ").append(topK).append(" 个片段编号。\n");
        sb.append("只输出 JSON 数组，例如 [3,0,7]，不要输出任何其他内容。");
        return sb.toString();
    }

    /**
     * 解析模型返回的下标数组。
     *
     * <p>模型输出不可控，可能带解释文字、返回越界下标或重复下标，
     * 这里统一做提取、去重、越界过滤和截断。
     *
     * @param candidateCount 候选总数，用于过滤越界下标
     */
    static List<Integer> parseIndices(String response, int candidateCount, int topK) {
        if (response == null || response.isBlank()) {
            return List.of();
        }

        Matcher matcher = INDEX_ARRAY.matcher(response);
        if (!matcher.find()) {
            return List.of();
        }

        String body = matcher.group(1);
        Set<Integer> seen = new LinkedHashSet<>();
        for (String token : body.split(",")) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                int index = Integer.parseInt(trimmed);
                if (index >= 0 && index < candidateCount) {
                    seen.add(index);
                }
            } catch (NumberFormatException ignored) {
                // 跳过非法 token，不影响其余下标
            }
            if (seen.size() >= topK) {
                break;
            }
        }
        return new ArrayList<>(seen);
    }

    private static String abbreviate(String s) {
        if (s == null) {
            return "null";
        }
        return s.length() > 120 ? s.substring(0, 120) + "..." : s;
    }

    /** 供测试注入配置 */
    void configure(boolean enabled, int snippetChars) {
        this.enabled = enabled;
        this.snippetChars = snippetChars;
    }
}
