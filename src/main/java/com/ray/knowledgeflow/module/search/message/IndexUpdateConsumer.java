package com.ray.knowledgeflow.module.search.message;

import com.ray.knowledgeflow.config.RabbitMQConfig;
import com.ray.knowledgeflow.module.ai.service.ChunkingService;
import com.ray.knowledgeflow.module.content.entity.ContentItem;
import com.ray.knowledgeflow.module.search.service.ISearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 索引更新消息消费者
 *
 * @author Ray
 * @since 2024-01-20
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IndexUpdateConsumer {

    private final ISearchService searchService;
    private final com.ray.knowledgeflow.module.search.service.EmbeddingService embeddingService;
    private final ChunkingService chunkingService;

    /**
     * 监听索引更新队列
     *
     * @param message 索引更新消息
     */
    @RabbitListener(queues = RabbitMQConfig.INDEX_UPDATE_QUEUE)
    public void handleIndexUpdateTask(IndexUpdateMessage message) {
        log.info("收到索引更新任务: action={}, type={}, id={}",
                message.getAction(), message.getDocumentType(), message.getDocumentId());

        try {
            switch (message.getAction().toUpperCase()) {
                case "CREATE":
                case "UPDATE": // CREATE 和 UPDATE 逻辑类似，都需要重新生成向量
                    handleUpsert(message);
                    break;

                case "DELETE":
                    handleDelete(message);
                    break;

                default:
                    log.warn("未知的索引操作类型: {}", message.getAction());
            }
        } catch (Exception e) {
            log.error("索引更新任务失败: action={}, type={}, id={}",
                    message.getAction(), message.getDocumentType(), message.getDocumentId(), e);
        }
    }

    /**
     * 处理新增或更新索引
     */
    private void handleUpsert(IndexUpdateMessage message) {
        log.info("开始更新向量索引: type={}, id={}", message.getDocumentType(), message.getDocumentId());

        // 处理 CONTENT_ITEM 类型
        if ("CONTENT_ITEM".equalsIgnoreCase(message.getDocumentType())) {
            // 1. 全文/语义搜索索引 (Whole Document)
            // 准备文本用于嵌入 (标题 + 内容)
            String textToEmbed = message.getTitle() + "\n" + message.getContent();

            // 生成向量
            java.util.List<Float> vector = embeddingService.embed(textToEmbed);
            if (!vector.isEmpty()) {
                // 构建 Qdrant Point
                io.qdrant.client.grpc.Points.PointStruct point = io.qdrant.client.grpc.Points.PointStruct.newBuilder()
                        .setId(io.qdrant.client.grpc.Points.PointId.newBuilder().setNum(message.getDocumentId())
                                .build())
                        .setVectors(io.qdrant.client.grpc.Points.Vectors.newBuilder()
                                .setVector(io.qdrant.client.grpc.Points.Vector.newBuilder().addAllData(vector).build())
                                .build())
                        .putPayload("sourceId",
                                io.qdrant.client.grpc.JsonWithInt.Value.newBuilder()
                                        .setIntegerValue(message.getDocumentId())
                                        .build())
                        .putPayload("title",
                                io.qdrant.client.grpc.JsonWithInt.Value.newBuilder().setStringValue(message.getTitle())
                                        .build())
                        .build();

                // Upsert 到 content 集合
                embeddingService.createCollection("content"); // 确保集合存在
                embeddingService.upsert("content", java.util.Collections.singletonList(point));
            }

            // 2. RAG 切片索引 (Chunks)
            // 构造临时的 ContentItem 对象
            ContentItem item = new ContentItem();
            item.setId(message.getDocumentId());
            item.setTitle(message.getTitle());
            item.setContentText(message.getContent()); // 注意映射到 contentText

            chunkingService.initCollection(); // 确保 chunk 集合存在
            chunkingService.indexContent(item);

            log.info("向量索引及切片更新完成: id={}", message.getDocumentId());
        }
        // 处理 NOTE 类型 - 笔记向量索引
        else if ("NOTE".equalsIgnoreCase(message.getDocumentType())) {
            chunkingService.initCollection(); // 确保 chunk 集合存在
            chunkingService.indexNote(
                    message.getDocumentId(),
                    message.getUserId(),
                    message.getTitle(),
                    message.getContent());
            log.info("笔记向量索引更新完成: id={}", message.getDocumentId());
        }
    }

    /**
     * 处理删除索引
     */
    private void handleDelete(IndexUpdateMessage message) {
        log.info("开始删除向量索引: type={}, id={}", message.getDocumentType(), message.getDocumentId());

        if ("CONTENT_ITEM".equalsIgnoreCase(message.getDocumentType())) {
            // 删除 content 集合中的整文档向量
            embeddingService.delete("content", message.getDocumentId());
            log.info("已删除整文档向量: id={}", message.getDocumentId());

            // 删除 content_chunks 集合中的切片
            chunkingService.deleteContentChunks(message.getDocumentId());
            log.info("已触发删除切片索引: id={}", message.getDocumentId());
        } else if ("NOTE".equalsIgnoreCase(message.getDocumentType())) {
            // 删除笔记的切片索引
            chunkingService.deleteNoteChunks(message.getDocumentId());
            log.info("已触发删除笔记切片索引: id={}", message.getDocumentId());
        }
    }
}
