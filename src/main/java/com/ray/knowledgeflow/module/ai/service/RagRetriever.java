package com.ray.knowledgeflow.module.ai.service;

import com.ray.knowledgeflow.module.ai.dto.NoteReferenceDto;
import com.ray.knowledgeflow.module.search.service.EmbeddingService;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

/**
 * RAG 检索器。
 *
 * <p>采用「召回 - 精排」两段式：向量检索先取回较多候选保证召回率，
 * 再由 {@link Reranker} 精排取前 K 个交给生成模型。
 * 只靠向量 top-K 时，相似度高但答非所问的片段会挤掉真正有用的内容。
 *
 * <p>检索逻辑原先在同步问答和流式问答里各写了一遍，此处统一。
 *
 * @author Ray
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagRetriever {

    private final EmbeddingService embeddingService;
    private final QdrantClient qdrantClient;
    private final Reranker reranker;

    private static final String CHUNK_COLLECTION = "content_chunks";

    /** 召回阶段候选数，需明显大于 topK 才有精排空间 */
    @Value("${app.rag.retrieve.candidate-size:20}")
    private int candidateSize;

    /** 精排后送入生成模型的片段数 */
    @Value("${app.rag.retrieve.top-k:5}")
    private int topK;

    /** 向量相似度下限 */
    @Value("${app.rag.retrieve.score-threshold:0.5}")
    private float scoreThreshold;

    /** Qdrant 单次召回上限，超时后降级为无上下文回答。 */
    @Value("${app.rag.retrieve.timeout-ms:5000}")
    private long timeoutMs = 5000;

    /**
     * 检索结果：拼装好的 prompt 与引用列表。
     */
    @Getter
    public static class RagContext {
        private final String prompt;
        private final List<NoteReferenceDto> references;

        RagContext(String prompt, List<NoteReferenceDto> references) {
            this.prompt = prompt;
            this.references = references;
        }

        public static RagContext of(String prompt, List<NoteReferenceDto> references) {
            return new RagContext(prompt, references);
        }

        /** 未检索到内容时，使用原始问题 */
        static RagContext empty(String query) {
            return new RagContext(query, new ArrayList<>());
        }
    }

    /**
     * 执行检索并拼装 prompt，检索与提问使用同一句话。首轮问答走这个重载。
     *
     * @param query          用户问题
     * @param userId         当前用户，用于隔离他人笔记
     * @param subTypeResolver 解析来源子类型的回调 (sourceId, sourceType) -> subType
     */
    public RagContext retrieve(String query, Long userId, BiFunction<Long, String, String> subTypeResolver) {
        return retrieve(query, query, userId, subTypeResolver);
    }

    /**
     * 执行检索并拼装 prompt。
     *
     * <p>检索与提问分开传：多轮追问经 {@link QueryRewriter} 消解指代后才能正确召回，
     * 但写进 prompt 的仍应是用户的原话——历史已经回灌给模型，指代由模型自行解析，
     * 没必要让用户看到一个被改写过的问题。
     *
     * @param retrievalQuery  用于 embedding 与精排的查询，需自包含
     * @param promptQuery     写进 prompt 的问题原文
     * @param userId          当前用户，用于隔离他人笔记
     * @param subTypeResolver 解析来源子类型的回调 (sourceId, sourceType) -> subType
     */
    public RagContext retrieve(String retrievalQuery, String promptQuery, Long userId,
            BiFunction<Long, String, String> subTypeResolver) {
        try {
            List<Float> vector = embeddingService.embed(retrievalQuery);
            if (vector.isEmpty()) {
                return RagContext.empty(promptQuery);
            }

            // 1. 召回：多取候选，给精排留出选择空间
            Points.SearchPoints searchPoints = Points.SearchPoints.newBuilder()
                    .setCollectionName(CHUNK_COLLECTION)
                    .addAllVector(vector)
                    .setLimit(candidateSize)
                    .setFilter(userFilter(userId))
                    .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(true).build())
                    .setScoreThreshold(scoreThreshold)
                    .build();

            List<Points.ScoredPoint> candidates = qdrantClient.searchAsync(searchPoints)
                    .get(timeoutMs, TimeUnit.MILLISECONDS);
            log.info("RAG 召回: query={}, candidates={}", retrievalQuery, candidates.size());

            if (candidates.isEmpty()) {
                return RagContext.empty(promptQuery);
            }

            // 2. 精排
            List<String> texts = candidates.stream()
                    .map(p -> payloadString(p, "content"))
                    .toList();
            List<Integer> ranked = reranker.rerank(retrievalQuery, texts, topK);
            log.info("RAG 精排: {} -> {}", candidates.size(), ranked.size());

            // 3. 拼装
            return build(promptQuery, candidates, ranked, subTypeResolver);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("RAG 检索被中断，降级为原始 prompt");
            return RagContext.empty(promptQuery);
        } catch (Exception e) {
            // 检索失败不应阻断问答，降级为无上下文回答
            log.warn("RAG 检索失败，降级为原始 prompt: {}", e.getMessage(), e);
            return RagContext.empty(promptQuery);
        }
    }

    private RagContext build(String query, List<Points.ScoredPoint> candidates,
            List<Integer> ranked, BiFunction<Long, String, String> subTypeResolver) {

        StringBuilder contextBuilder = new StringBuilder("以下是参考资料：\n\n");
        List<NoteReferenceDto> references = new ArrayList<>();
        Set<Long> addedSourceIds = new HashSet<>();

        for (Integer index : ranked) {
            Points.ScoredPoint point = candidates.get(index);
            String content = payloadString(point, "content");
            String title = payloadString(point, "title");
            long sourceId = point.getPayloadMap().containsKey("sourceId")
                    ? point.getPayloadMap().get("sourceId").getIntegerValue()
                    : 0L;
            String sourceType = point.getPayloadMap().containsKey("sourceType")
                    ? point.getPayloadMap().get("sourceType").getStringValue()
                    : "CONTENT";

            contextBuilder.append("--- [").append(title).append("] ---\n")
                    .append(content).append("\n\n");

            if (addedSourceIds.add(sourceId)) {
                String subType = point.getPayloadMap().containsKey("subType")
                        ? point.getPayloadMap().get("subType").getStringValue()
                        : subTypeResolver.apply(sourceId, sourceType);

                references.add(new NoteReferenceDto(
                        sourceId,
                        title,
                        content.length() > 100 ? content.substring(0, 100) + "..." : content,
                        (double) point.getScore(),
                        sourceType,
                        subType));
            }
        }

        contextBuilder.append("请基于以上参考资料回答用户的问题。如果资料不足，请用你的通用知识补充。问题：\n")
                .append(query);

        return new RagContext(contextBuilder.toString(), references);
    }

    private static String payloadString(Points.ScoredPoint point, String key) {
        return point.getPayloadMap().containsKey(key)
                ? point.getPayloadMap().get(key).getStringValue()
                : "";
    }

    /**
     * 用户维度过滤，防止检索到他人笔记
     */
    private static Points.Filter userFilter(Long userId) {
        return Points.Filter.newBuilder()
                .addMust(Points.Condition.newBuilder()
                        .setField(Points.FieldCondition.newBuilder()
                                .setKey("userId")
                                .setMatch(Points.Match.newBuilder()
                                        .setInteger(userId)
                                        .build())
                                .build())
                        .build())
                .build();
    }
}
