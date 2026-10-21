package com.ray.knowledgeflow.module.search.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.UpdateResult;
import io.qdrant.client.grpc.Points.Filter; // Added import for Filter
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * 文本嵌入与向量服务
 *
 * @author Ray
 * @since 2024-01-21
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final QdrantClient qdrantClient;

    @Value("${qdrant.operation-timeout-ms:5000}")
    private long operationTimeoutMs = 5000;

    /**
     * 生成文本的向量嵌入
     *
     * @param text 文本内容
     * @return 向量列表 (Float)
     */
    public List<Float> embed(String text) {
        if (text == null || text.trim().isEmpty()) {
            return List.of();
        }

        // 生成嵌入
        Embedding embedding = embeddingModel.embed(text).content();
        return embedding.vectorAsList();
    }

    /**
     * 批量生成文本嵌入。
     *
     * <p>语义切片需要为每个句子求向量，逐条调用会产生几十次网络往返，
     * 这里合并为一次批量请求。返回顺序与入参一一对应。
     *
     * @param texts 文本列表
     * @return 与入参等长的向量列表；整体失败时返回空列表，由调用方降级处理
     */
    public List<List<Float>> embedAll(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        try {
            List<TextSegment> segments = texts.stream()
                    .map(TextSegment::from)
                    .collect(Collectors.toList());

            List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
            return embeddings.stream()
                    .map(Embedding::vectorAsList)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("批量生成嵌入失败, size={}", texts.size(), e);
            return List.of();
        }
    }

    /**
     * 创建 Qdrant 集合 (如果不存在)
     *
     * @param collectionName 集合名称
     * @param vectorSize     向量维度 (默认 384)
     */
    public void createCollection(String collectionName, int vectorSize) {
        try {
            boolean exists = qdrantClient.listCollectionsAsync().get(operationTimeoutMs, TimeUnit.MILLISECONDS)
                    .contains(collectionName);
            if (!exists) {
                qdrantClient.createCollectionAsync(
                        collectionName,
                        VectorParams.newBuilder()
                                .setDistance(Distance.Cosine)
                                .setSize(vectorSize)
                                .build())
                        .get(operationTimeoutMs, TimeUnit.MILLISECONDS);
                log.info("Created Qdrant collection: {}", collectionName);
            }
        } catch (InterruptedException e) {
            log.error("Failed to create collection: {}", collectionName, e);
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Qdrant collection creation interrupted", e);
        } catch (ExecutionException | TimeoutException e) {
            log.error("Failed to create collection: {}", collectionName, e);
            throw new IllegalStateException("Qdrant collection creation failed", e);
        }
    }

    /**
     * 创建 Qdrant 集合 (使用默认维度 384)
     *
     * @param collectionName 集合名称
     */
    public void createCollection(String collectionName) {
        createCollection(collectionName, 2048);
    }

    /**
     * 批量插入或更新向量
     *
     * @param collectionName 集合名称
     * @param points         向量点列表
     */
    public void upsert(String collectionName, List<PointStruct> points) {
        if (points == null || points.isEmpty()) {
            return;
        }

        try {
            UpdateResult result = qdrantClient.upsertAsync(collectionName, points)
                    .get(operationTimeoutMs, TimeUnit.MILLISECONDS);
            log.debug("Upserted {} points to {}, status: {}", points.size(), collectionName, result.getStatus());
        } catch (InterruptedException e) {
            log.error("Failed to upsert points to {}", collectionName, e);
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Qdrant upsert interrupted", e);
        } catch (ExecutionException | TimeoutException e) {
            log.error("Failed to upsert points to {}", collectionName, e);
            throw new IllegalStateException("Qdrant upsert failed", e);
        }
    }

    /**
     * 从集合中删除指定 ID 的点
     *
     * @param collectionName 集合名称
     * @param id             点 ID
     */
    public void delete(String collectionName, Long id) {
        if (id == null) {
            return;
        }
        // 使用 filter 方式删除，避免不同版本的 PointIdsList 兼容性问题
        Filter filter = Filter.newBuilder()
                .addMust(io.qdrant.client.grpc.Points.Condition.newBuilder()
                        .setField(io.qdrant.client.grpc.Points.FieldCondition.newBuilder()
                                .setKey("sourceId")
                                .setMatch(io.qdrant.client.grpc.Points.Match.newBuilder()
                                        .setInteger(id)
                                        .build())
                                .build())
                        .build())
                .build();
        deleteByFilter(collectionName, filter);
    }

    /**
     * 根据过滤器删除向量
     *
     * @param collectionName 集合名称
     * @param filter         过滤器
     */
    public void deleteByFilter(String collectionName, io.qdrant.client.grpc.Points.Filter filter) {
        try {
            qdrantClient.deleteAsync(
                    io.qdrant.client.grpc.Points.DeletePoints.newBuilder()
                            .setCollectionName(collectionName)
                            .setPoints(io.qdrant.client.grpc.Points.PointsSelector.newBuilder()
                                    .setFilter(filter)
                                    .build())
                            .build())
                    .get(operationTimeoutMs, TimeUnit.MILLISECONDS);
            log.debug("Deleted points by filter from {}", collectionName);
        } catch (InterruptedException e) {
            log.error("Failed to delete points by filter from {}", collectionName, e);
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Qdrant delete interrupted", e);
        } catch (ExecutionException | TimeoutException e) {
            log.error("Failed to delete points by filter from {}", collectionName, e);
            throw new IllegalStateException("Qdrant delete failed", e);
        }
    }
}
