package com.ray.knowledgeflow.module.ai.service;

import java.util.ArrayList;
import java.util.List;

/**
 * 检索结果重排器。
 *
 * <p>向量召回按语义相似度排序，但"相似"不等于"能回答问题"。重排阶段用交互式模型
 * 重新判断每个候选与问题的相关性，把精排从 bi-encoder 换成表达能力更强的模型。
 *
 * <p>实现可替换：交叉编码器（精度高、延迟低）、大模型列表式（无需额外服务）等。
 * 所有实现都必须保证失败时退回向量原始顺序——重排是增强，不是阻断。
 *
 * @author Ray
 */
public interface Reranker {

    /**
     * 重排候选片段。
     *
     * @param query      用户问题
     * @param candidates 候选片段，顺序为向量检索的原始顺序
     * @param topK       需要保留的数量
     * @return 重排后的下标列表，元素为 candidates 的索引
     */
    List<Integer> rerank(String query, List<String> candidates, int topK);

    /**
     * 退化顺序：保持向量检索原始排序
     */
    static List<Integer> naturalOrder(int size) {
        List<Integer> indices = new ArrayList<>(Math.max(size, 0));
        for (int i = 0; i < size; i++) {
            indices.add(i);
        }
        return indices;
    }
}
