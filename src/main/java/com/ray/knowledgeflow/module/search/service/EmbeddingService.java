package com.ray.knowledgeflow.module.search.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.UpdateResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;

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
     * 创建 Qdrant 集合 (如果不存在)
     *
     * @param collectionName 集合名称
     * @param vectorSize     向量维度 (默认 384)
     */
    public void createCollection(String collectionName, int vectorSize) {
        try {
            boolean exists = qdrantClient.listCollectionsAsync().get()
                    .contains(collectionName);
            if (!exists) {
                qdrantClient.createCollectionAsync(
                        collectionName,
                        VectorParams.newBuilder()
                                .setDistance(Distance.Cosine)
                                .setSize(vectorSize)
                                .build())
                        .get();
                log.info("Created Qdrant collection: {}", collectionName);
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to create collection: {}", collectionName, e);
            Thread.currentThread().interrupt();
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
            UpdateResult result = qdrantClient.upsertAsync(collectionName, points).get();
            log.debug("Upserted {} points to {}, status: {}", points.size(), collectionName, result.getStatus());
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to upsert points to {}", collectionName, e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 从集合中删除指定 ID 的点
     *
     * @param collectionName 集合名称
     * @param id             点 ID
     */
    public void delete(String collectionName, Long id) {
        // TODO: Fix Qdrant delete logic. Compilation failed due to missing PointIdsList
        // class.
        // Needs to investigate correct Qdrant Java Client 1.7.0 gRPC class structure.
        log.warn("Qdrant delete operation skipped for id {} in collection {} due to compilation issues", id,
                collectionName);
        /*
         * try {
         * qdrantClient.deleteAsync(
         * io.qdrant.client.grpc.Points.DeletePoints.newBuilder()
         * .setCollectionName(collectionName)
         * .setPoints(io.qdrant.client.grpc.Points.PointsSelector.newBuilder()
         * .setPoints(io.qdrant.client.grpc.PointIdsList.newBuilder()
         * .addIds(io.qdrant.client.grpc.PointId.newBuilder()
         * .setNum(id)
         * .build())
         * .build())
         * .build())
         * .build())
         * .get();
         * log.debug("Deleted point {} from {}", id, collectionName);
         * } catch (InterruptedException | ExecutionException e) {
         * log.error("Failed to delete point {} from {}", id, collectionName, e);
         * Thread.currentThread().interrupt();
         * }
         */
    }
}
