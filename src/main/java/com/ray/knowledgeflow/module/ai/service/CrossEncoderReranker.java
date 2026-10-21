package com.ray.knowledgeflow.module.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 交叉编码器重排器（推荐 BAAI/bge-reranker-v2-m3）。
 *
 * <p>交叉编码器把 query 和候选文档拼在一起过一次模型，能捕捉两者的细粒度交互，
 * 精度显著高于召回阶段的 bi-encoder；代价是无法预计算，因此只用于候选集精排。
 * 相比大模型列表式重排，专用重排模型体积小一到两个数量级，延迟通常低一个数量级。
 *
 * <p>请求体采用业界通行的 Cohere 风格；响应解析同时兼容
 * {@code {"results":[{"index":0,"relevance_score":0.9}]}}（Cohere / Jina / 硅基流动等）
 * 与 {@code [{"index":0,"score":0.9}]}（HuggingFace TEI）两种形状，
 * 因此本地自部署与托管 API 之间切换只需改配置。
 *
 * <p>各家字段细节可能随版本变动，接入时请对照所选服务的当前文档核对。
 *
 * @author Ray
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.rag.rerank.provider", havingValue = "cross-encoder", matchIfMissing = true)
public class CrossEncoderReranker implements Reranker {

    private final ObjectMapper objectMapper;

    /** 重排服务地址，如 http://localhost:8080/rerank（TEI）或厂商的 /v1/rerank */
    @Value("${app.rag.rerank.base-url:http://localhost:8080/rerank}")
    private String baseUrl;

    /** 模型名。自部署 TEI 时该字段被忽略，托管 API 必填 */
    @Value("${app.rag.rerank.model:BAAI/bge-reranker-v2-m3}")
    private String model;

    /** API Key，自部署可留空 */
    @Value("${app.rag.rerank.api-key:}")
    private String apiKey;

    /**
     * 默认给本机 MPS 长文本重排留出尾延迟余量；可通过 app.rag.rerank.timeout-ms 覆盖。
     */
    @Value("${app.rag.rerank.timeout-ms:3500}")
    private long timeoutMs;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(1))
            .build();

    @Override
    public List<Integer> rerank(String query, List<String> candidates, int topK) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        // 候选还没 topK 多时重排没有意义，省下一次网络往返
        if (candidates.size() <= topK) {
            return Reranker.naturalOrder(candidates.size());
        }

        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(query, candidates, topK)));

            if (apiKey != null && !apiKey.isBlank()) {
                builder.header("Authorization", "Bearer " + apiKey);
            }

            HttpResponse<String> response = httpClient.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("重排服务返回 {}，退回向量顺序: {}", response.statusCode(), abbreviate(response.body()));
                return Reranker.naturalOrder(topK);
            }

            List<Integer> ranked = parseIndices(objectMapper, response.body(), candidates.size(), topK);
            if (ranked.isEmpty()) {
                log.warn("重排响应解析为空，退回向量顺序: {}", abbreviate(response.body()));
                return Reranker.naturalOrder(topK);
            }
            return ranked;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Reranker.naturalOrder(topK);
        } catch (Exception e) {
            log.warn("重排调用失败，退回向量顺序: {}", e.getMessage());
            return Reranker.naturalOrder(topK);
        }
    }

    // ==================== 纯函数，便于单独测试 ====================

    String buildRequestBody(String query, List<String> candidates, int topK) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.put("query", query);
        root.put("top_n", topK);

        ArrayNode documents = root.putArray("documents");
        candidates.forEach(documents::add);
        // TEI 使用 texts 字段，一并带上以兼容自部署
        ArrayNode texts = root.putArray("texts");
        candidates.forEach(texts::add);

        return root.toString();
    }

    /**
     * 解析重排响应，兼容两种主流形状。
     *
     * <p>结果按服务返回顺序（相关性降序）取下标，并做越界过滤与去重——
     * 外部服务的返回值不可信，越界下标会直接导致数组访问异常。
     */
    static List<Integer> parseIndices(ObjectMapper mapper, String body, int candidateCount, int topK) {
        if (body == null || body.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = mapper.readTree(body);
            JsonNode results = root.isArray() ? root : root.path("results");
            if (!results.isArray()) {
                return List.of();
            }

            Set<Integer> ordered = new LinkedHashSet<>();
            for (JsonNode item : results) {
                JsonNode indexNode = item.path("index");
                if (!indexNode.isInt()) {
                    continue;
                }
                int index = indexNode.asInt();
                if (index >= 0 && index < candidateCount) {
                    ordered.add(index);
                }
                if (ordered.size() >= topK) {
                    break;
                }
            }
            return new ArrayList<>(ordered);
        } catch (Exception e) {
            return List.of();
        }
    }

    private static String abbreviate(String s) {
        if (s == null) {
            return "null";
        }
        return s.length() > 120 ? s.substring(0, 120) + "..." : s;
    }

    /** 供测试注入配置 */
    void configure(String baseUrl, String model, String apiKey, long timeoutMs) {
        this.baseUrl = baseUrl;
        this.model = model;
        this.apiKey = apiKey;
        this.timeoutMs = timeoutMs;
    }
}
