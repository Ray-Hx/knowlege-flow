package com.ray.knowledgeflow.module.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 交叉编码器重排器测试。
 *
 * <p>重点覆盖外部服务返回值的不可信性，以及托管 API 与自部署 TEI 两种响应形状。
 */
class CrossEncoderRerankerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    // ==================== 响应解析：两种主流形状 ====================

    @Test
    void parseIndices_CohereStyle_WithResultsWrapper() {
        String body = """
                {"results":[
                  {"index":3,"relevance_score":0.98},
                  {"index":0,"relevance_score":0.71}
                ]}""";

        assertEquals(List.of(3, 0), CrossEncoderReranker.parseIndices(mapper, body, 6, 5));
    }

    /**
     * 契约测试：报文取自本地 reranker-service 的真实响应，
     * 两端字段一旦不一致，这里会先失败。
     */
    @Test
    void parseIndices_LocalRerankerServiceResponse() {
        String body = """
                {"results": [{"index": 1, "relevance_score": 0.95}, {"index": 2, "relevance_score": 0.5}]}""";

        assertEquals(List.of(1, 2), CrossEncoderReranker.parseIndices(mapper, body, 4, 2));
    }

    @Test
    void parseIndices_TeiStyle_BareArray() {
        // HuggingFace TEI 直接返回数组，字段名是 score
        String body = """
                [{"index":2,"score":0.9},{"index":5,"score":0.4}]""";

        assertEquals(List.of(2, 5), CrossEncoderReranker.parseIndices(mapper, body, 6, 5));
    }

    // ==================== 外部返回值不可信 ====================

    @Test
    void parseIndices_ShouldDropOutOfRangeIndices() {
        // 越界下标若不过滤，会在按下标取候选时直接数组越界
        String body = """
                {"results":[{"index":99},{"index":1},{"index":-2}]}""";

        assertEquals(List.of(1), CrossEncoderReranker.parseIndices(mapper, body, 6, 5));
    }

    @Test
    void parseIndices_ShouldDeduplicate() {
        String body = """
                {"results":[{"index":2},{"index":2},{"index":0}]}""";

        assertEquals(List.of(2, 0), CrossEncoderReranker.parseIndices(mapper, body, 6, 5));
    }

    @Test
    void parseIndices_ShouldTruncateToTopK() {
        String body = """
                {"results":[{"index":0},{"index":1},{"index":2},{"index":3}]}""";

        assertEquals(List.of(0, 1), CrossEncoderReranker.parseIndices(mapper, body, 6, 2));
    }

    @Test
    void parseIndices_MalformedPayloads_ShouldReturnEmpty() {
        assertTrue(CrossEncoderReranker.parseIndices(mapper, "not json at all", 6, 5).isEmpty());
        assertTrue(CrossEncoderReranker.parseIndices(mapper, "{\"error\":\"quota exceeded\"}", 6, 5).isEmpty());
        assertTrue(CrossEncoderReranker.parseIndices(mapper, "", 6, 5).isEmpty());
        assertTrue(CrossEncoderReranker.parseIndices(mapper, null, 6, 5).isEmpty());
    }

    @Test
    void parseIndices_MissingIndexField_ShouldSkipEntry() {
        String body = """
                {"results":[{"relevance_score":0.9},{"index":4}]}""";

        assertEquals(List.of(4), CrossEncoderReranker.parseIndices(mapper, body, 6, 5));
    }

    // ==================== 请求构造 ====================

    @Test
    void buildRequestBody_ShouldCarryModelQueryAndBothDocumentFields() throws Exception {
        CrossEncoderReranker reranker = new CrossEncoderReranker(mapper);
        reranker.configure("http://x/rerank", "BAAI/bge-reranker-v2-m3", "", 2000);

        JsonNode body = mapper.readTree(
                reranker.buildRequestBody("什么是GC", List.of("片段A", "片段B"), 2));

        assertEquals("BAAI/bge-reranker-v2-m3", body.path("model").asText());
        assertEquals("什么是GC", body.path("query").asText());
        assertEquals(2, body.path("top_n").asInt());
        // documents 供托管 API，texts 供自部署 TEI，两者都带上以便切换
        assertEquals(2, body.path("documents").size());
        assertEquals(2, body.path("texts").size());
        assertEquals("片段A", body.path("documents").get(0).asText());
    }

    // ==================== 短路与降级 ====================

    @Test
    void rerank_WhenCandidatesFewerThanTopK_ShouldSkipNetworkCall() {
        // base-url 指向不可达地址，若真发起请求会超时失败
        CrossEncoderReranker reranker = new CrossEncoderReranker(mapper);
        reranker.configure("http://127.0.0.1:1/rerank", "m", "", 100);

        assertEquals(List.of(0, 1), reranker.rerank("问题", List.of("a", "b"), 5));
    }

    @Test
    void rerank_WhenServiceUnreachable_ShouldFallBackToVectorOrder() {
        CrossEncoderReranker reranker = new CrossEncoderReranker(mapper);
        // 保留端口 1，连接必然失败
        reranker.configure("http://127.0.0.1:1/rerank", "m", "", 300);

        List<String> candidates = List.of("a", "b", "c", "d", "e");

        // 重排服务不可用时不能阻断问答
        assertEquals(List.of(0, 1, 2), reranker.rerank("问题", candidates, 3));
    }

    @Test
    void rerank_EmptyCandidates_ShouldReturnEmpty() {
        CrossEncoderReranker reranker = new CrossEncoderReranker(mapper);
        reranker.configure("http://127.0.0.1:1/rerank", "m", "", 100);

        assertTrue(reranker.rerank("问题", List.of(), 3).isEmpty());
    }
}
