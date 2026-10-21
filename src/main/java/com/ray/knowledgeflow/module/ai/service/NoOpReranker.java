package com.ray.knowledgeflow.module.ai.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 空重排器：保持向量召回的原始顺序。
 *
 * <p>存在的意义是做对照组——关闭重排跑一遍评测集，才能量化重排到底带来了多少提升。
 *
 * @author Ray
 */
@Component
@ConditionalOnProperty(name = "app.rag.rerank.provider", havingValue = "none")
public class NoOpReranker implements Reranker {

    @Override
    public List<Integer> rerank(String query, List<String> candidates, int topK) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        return Reranker.naturalOrder(Math.min(candidates.size(), topK));
    }
}
