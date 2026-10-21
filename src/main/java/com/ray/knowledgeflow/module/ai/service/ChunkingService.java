package com.ray.knowledgeflow.module.ai.service;

import com.ray.knowledgeflow.module.content.entity.ContentItem;
import com.ray.knowledgeflow.module.search.service.EmbeddingService;
import dev.langchain4j.data.segment.TextSegment;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
        private final SemanticChunker semanticChunker;
        private static final String CHUNK_COLLECTION = "content_chunks";

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
                        String segmentText = segment.text();
                        // 注入元数据到待嵌入文本中，但不改变存储的 payload 内容
                        String textToEmbed = String.format("标题：%s 摘要：%s 标签：%s\n%s",
                                        item.getTitle() != null ? item.getTitle() : "",
                                        item.getSummary() != null ? item.getSummary() : "",
                                        item.getTags() != null ? item.getTags() : "",
                                        segmentText);

                        // 嵌入包含了元数据的文本
                        List<Float> vector = embeddingService.embed(textToEmbed);
                        if (vector.isEmpty()) {
                                continue;
                        }

                        // 构建 Payload - 存储的 content 仍然是原始分块文本
                        Points.PointStruct point = Points.PointStruct.newBuilder()
                                        .setId(Points.PointId.newBuilder().setUuid(UUID.randomUUID().toString())
                                                        .build())
                                        .setVectors(Points.Vectors.newBuilder()
                                                        .setVector(Points.Vector.newBuilder().addAllData(vector)
                                                                        .build()))
                                        .putPayload("content",
                                                        JsonWithInt.Value.newBuilder().setStringValue(segmentText)
                                                                        .build())
                                        .putPayload("sourceId",
                                                        JsonWithInt.Value.newBuilder()
                                                                        .setIntegerValue(item.getId())
                                                                        .build())
                                        .putPayload("userId",
                                                        JsonWithInt.Value.newBuilder()
                                                                        .setIntegerValue(item.getUserId())
                                                                        .build())
                                        .putPayload("title",
                                                        JsonWithInt.Value.newBuilder()
                                                                        .setStringValue(item.getTitle() != null
                                                                                        ? item.getTitle()
                                                                                        : "")
                                                                        .build())
                                        .putPayload("url",
                                                        JsonWithInt.Value.newBuilder().setStringValue(
                                                                        item.getUrl() != null ? item.getUrl() : "")
                                                                        .build())
                                        .putPayload("author",
                                                        JsonWithInt.Value.newBuilder()
                                                                        .setStringValue(item.getAuthor() != null
                                                                                        ? item.getAuthor()
                                                                                        : "")
                                                                        .build())
                                        .putPayload("publishTime",
                                                        JsonWithInt.Value.newBuilder()
                                                                        .setStringValue(
                                                                                        item.getPublishTime() != null
                                                                                                        ? item.getPublishTime()
                                                                                                                        .toString()
                                                                                                        : "")
                                                                        .build())
                                        .putPayload("sourceType",
                                                        JsonWithInt.Value.newBuilder().setStringValue("CONTENT")
                                                                        .build())
                                        .putPayload("subType",
                                                        JsonWithInt.Value.newBuilder().setStringValue(
                                                                        item.getSourceId() == null ? "UPLOAD" : "FEED")
                                                                        .build())
                                        .build();

                        points.add(point);
                }

                if (points.isEmpty()) {
                        log.warn("内容未生成有效向量，保留原索引: contentId={}", item.getId());
                        return;
                }

                // 新向量全部生成成功后再替换旧切片，避免外部服务故障时先删后失败。
                deleteContentChunks(item.getId());
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
                Points.Filter filter = Points.Filter.newBuilder()
                                .addMust(Points.Condition.newBuilder()
                                                .setField(Points.FieldCondition.newBuilder()
                                                                .setKey("sourceId")
                                                                .setMatch(Points.Match.newBuilder()
                                                                                .setInteger(sourceId)
                                                                                .build())
                                                                .build())
                                                .build())
                                .addMust(Points.Condition.newBuilder()
                                                .setField(Points.FieldCondition.newBuilder()
                                                                .setKey("sourceType")
                                                                .setMatch(Points.Match.newBuilder()
                                                                                .setText("CONTENT")
                                                                                .build())
                                                                .build())
                                                .build())
                                .build();
                embeddingService.deleteByFilter(CHUNK_COLLECTION, filter);
                log.info("删除知识库切片索引: sourceId={}", sourceId);
        }

        /**
         * 索引笔记：分块 -> 嵌入 -> 存储
         * 改进：将标题和内容组合后生成向量，这样标题中的关键词也能被检索到
         *
         * @param noteId    笔记ID
         * @param userId    用户ID
         * @param title     笔记标题
         * @param contentMd 笔记 Markdown 内容
         * @param summary   笔记摘要
         * @param tags      笔记标签
         */
        public void indexNote(Long noteId, Long userId, String title, String contentMd, String summary, String tags) {
                if (noteId == null || contentMd == null || contentMd.isEmpty()) {
                        return;
                }

                // 1. 分块 (只对正文进行分块)
                List<TextSegment> segments = splitText(contentMd);
                log.info("Split note id={} into {} chunks", noteId, segments.size());

                if (segments.isEmpty()) {
                        return;
                }

                List<Points.PointStruct> points = new ArrayList<>();

                // 3. 处理每个分块
                for (TextSegment segment : segments) {
                        String segmentText = segment.text();
                        // 注入元数据到待嵌入文本中
                        String textToEmbed = String.format("标题：%s 摘要：%s 标签：%s\n%s",
                                        title != null ? title : "",
                                        summary != null ? summary : "",
                                        tags != null ? tags : "",
                                        segmentText);

                        // 嵌入（包含元数据信息的文本）
                        List<Float> vector = embeddingService.embed(textToEmbed);
                        if (vector.isEmpty()) {
                                continue;
                        }

                        // 构建 Payload - 使用 sourceType 区分笔记和内容项
                        Points.PointStruct point = Points.PointStruct.newBuilder()
                                        .setId(Points.PointId.newBuilder().setUuid(UUID.randomUUID().toString())
                                                        .build())
                                        .setVectors(Points.Vectors.newBuilder()
                                                        .setVector(Points.Vector.newBuilder().addAllData(vector)
                                                                        .build()))
                                        .putPayload("content",
                                                        JsonWithInt.Value.newBuilder().setStringValue(segmentText)
                                                                        .build())
                                        .putPayload("sourceId",
                                                        JsonWithInt.Value.newBuilder()
                                                                        .setIntegerValue(noteId).build())
                                        .putPayload("userId",
                                                        JsonWithInt.Value.newBuilder()
                                                                        .setIntegerValue(userId).build())
                                        .putPayload("title",
                                                        JsonWithInt.Value.newBuilder()
                                                                        .setStringValue(title != null ? title : "")
                                                                        .build())
                                        .putPayload("sourceType",
                                                        JsonWithInt.Value.newBuilder().setStringValue("NOTE").build())
                                        .putPayload("subType",
                                                        JsonWithInt.Value.newBuilder().setStringValue("NOTE").build())
                                        .build();

                        points.add(point);
                }

                if (points.isEmpty()) {
                        log.warn("笔记未生成有效向量，保留原索引: noteId={}", noteId);
                        return;
                }

                // 新向量全部生成成功后再替换旧切片，缩小更新失败导致索引完全缺失的窗口。
                deleteNoteChunks(noteId);
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
                Points.Filter filter = Points.Filter.newBuilder()
                                .addMust(Points.Condition.newBuilder()
                                                .setField(Points.FieldCondition.newBuilder()
                                                                .setKey("sourceId")
                                                                .setMatch(Points.Match.newBuilder()
                                                                                .setInteger(noteId)
                                                                                .build())
                                                                .build())
                                                .build())
                                .addMust(Points.Condition.newBuilder()
                                                .setField(Points.FieldCondition.newBuilder()
                                                                .setKey("sourceType")
                                                                .setMatch(Points.Match.newBuilder()
                                                                                .setText("NOTE")
                                                                                .build())
                                                                .build())
                                                .build())
                                .build();
                embeddingService.deleteByFilter(CHUNK_COLLECTION, filter);
                log.info("删除笔记切片索引: noteId={}", noteId);
        }

        /**
         * 文本分块。
         *
         * <p>按语义边界切分而非固定长度，避免把一段完整论述从中间截断
         * 导致检索到的片段缺失上下文。
         *
         * @param text 原始文本
         * @return 分块列表
         */
        private List<TextSegment> splitText(String text) {
                return semanticChunker.chunk(text).stream()
                                .map(TextSegment::from)
                                .collect(Collectors.toList());
        }
}
