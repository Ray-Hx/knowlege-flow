package com.ray.knowledgeflow.module.ai.service;

import com.ray.knowledgeflow.module.content.entity.ContentItem;
import com.ray.knowledgeflow.module.search.service.EmbeddingService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 文档分块服务
 *
 * @author Ray
 * @since 2024-01-21
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChunkingService {

    private final EmbeddingService embeddingService;
    private static final String CHUNK_COLLECTION = "content_chunks";
    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 50;

    /**
     * 初始化集合
     */
    public void initCollection() {
        embeddingService.createCollection(CHUNK_COLLECTION, 2048);
    }

    /**
     * 索引内容：分块 -> 嵌入 -> 存储
     *
     * @param item 内容项
     */
    public void indexContent(ContentItem item) {
        if (item == null || item.getContentText() == null || item.getContentText().isEmpty()) {
            return;
        }

        // 1. 分块
        List<TextSegment> segments = splitText(item.getContentText());
        log.info("Split content id={} into {} chunks", item.getId(), segments.size());

        if (segments.isEmpty()) {
            return;
        }

        List<Points.PointStruct> points = new ArrayList<>();

        // 2. 处理每个分块
        for (TextSegment segment : segments) {
            String text = segment.text();
            // 嵌入
            List<Float> vector = embeddingService.embed(text);
            if (vector.isEmpty()) {
                continue;
            }

            // 构建 Payload
            Points.PointStruct point = Points.PointStruct.newBuilder()
                    .setId(Points.PointId.newBuilder().setUuid(UUID.randomUUID().toString()).build())
                    .setVectors(Points.Vectors.newBuilder()
                            .setVector(Points.Vector.newBuilder().addAllData(vector).build()))
                    .putPayload("content", JsonWithInt.Value.newBuilder().setStringValue(text).build())
                    .putPayload("sourceId",
                            JsonWithInt.Value.newBuilder().setIntegerValue(item.getId().intValue()).build())
                    .putPayload("title",
                            JsonWithInt.Value.newBuilder()
                                    .setStringValue(item.getTitle() != null ? item.getTitle() : "").build())
                    .putPayload("url",
                            JsonWithInt.Value.newBuilder().setStringValue(item.getUrl() != null ? item.getUrl() : "")
                                    .build())
                    .putPayload("author",
                            JsonWithInt.Value.newBuilder()
                                    .setStringValue(item.getAuthor() != null ? item.getAuthor() : "").build())
                    .putPayload("publishTime",
                            JsonWithInt.Value.newBuilder()
                                    .setStringValue(
                                            item.getPublishTime() != null ? item.getPublishTime().toString() : "")
                                    .build())
                    .build();

            points.add(point);
        }

        // 3. 批量插入
        embeddingService.upsert(CHUNK_COLLECTION, points);
    }

    /**
     * 删除内容的切片索引
     *
     * @param sourceId 内容源ID
     */
    public void deleteContentChunks(Long sourceId) {
        if (sourceId == null) {
            return;
        }
        // TODO: 使用 deletePayload 或 filter 删除
        // 暂时留空，等待 EmbeddingService 支持 deleteByFilter
    }

    /**
     * 索引笔记：分块 -> 嵌入 -> 存储
     * 改进：将标题和内容组合后生成向量，这样标题中的关键词也能被检索到
     *
     * @param noteId    笔记ID
     * @param userId    用户ID
     * @param title     笔记标题
     * @param contentMd 笔记 Markdown 内容
     */
    public void indexNote(Long noteId, Long userId, String title, String contentMd) {
        if (noteId == null || contentMd == null || contentMd.isEmpty()) {
            return;
        }

        // 1. 组合标题和内容用于分块和向量化
        // 这样标题中的关键词也能被检索到
        String titlePrefix = (title != null && !title.isEmpty()) ? "# " + title + "\n\n" : "";
        String fullText = titlePrefix + contentMd;

        // 2. 分块
        List<TextSegment> segments = splitText(fullText);
        log.info("Split note id={} into {} chunks", noteId, segments.size());

        if (segments.isEmpty()) {
            return;
        }

        List<Points.PointStruct> points = new ArrayList<>();

        // 3. 处理每个分块
        for (TextSegment segment : segments) {
            String text = segment.text();
            // 嵌入（包含标题信息的文本）
            List<Float> vector = embeddingService.embed(text);
            if (vector.isEmpty()) {
                continue;
            }

            // 构建 Payload - 使用 sourceType 区分笔记和内容项
            Points.PointStruct point = Points.PointStruct.newBuilder()
                    .setId(Points.PointId.newBuilder().setUuid(UUID.randomUUID().toString()).build())
                    .setVectors(Points.Vectors.newBuilder()
                            .setVector(Points.Vector.newBuilder().addAllData(vector).build()))
                    .putPayload("content", JsonWithInt.Value.newBuilder().setStringValue(text).build())
                    .putPayload("sourceId",
                            JsonWithInt.Value.newBuilder().setIntegerValue(noteId.intValue()).build())
                    .putPayload("userId",
                            JsonWithInt.Value.newBuilder().setIntegerValue(userId.intValue()).build())
                    .putPayload("title",
                            JsonWithInt.Value.newBuilder()
                                    .setStringValue(title != null ? title : "").build())
                    .putPayload("sourceType",
                            JsonWithInt.Value.newBuilder().setStringValue("NOTE").build())
                    .build();

            points.add(point);
        }

        // 3. 批量插入
        embeddingService.upsert(CHUNK_COLLECTION, points);
        log.info("笔记向量索引完成: noteId={}, chunks={}", noteId, points.size());
    }

    /**
     * 删除笔记的切片索引
     *
     * @param noteId 笔记ID
     */
    public void deleteNoteChunks(Long noteId) {
        if (noteId == null) {
            return;
        }
        // TODO: 使用 filter 删除 sourceType=NOTE && sourceId=noteId 的向量
        log.info("删除笔记切片索引: noteId={}", noteId);
    }

    /**
     * 文本分块
     *
     * @param text 原始文本
     * @return 分块列表
     */
    private List<TextSegment> splitText(String text) {
        DocumentSplitter splitter = DocumentSplitters.recursive(CHUNK_SIZE, CHUNK_OVERLAP);
        return splitter.split(Document.from(text));
    }
}
