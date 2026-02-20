package com.ray.knowledgeflow.module.search.message;

import com.ray.knowledgeflow.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 索引更新消息生产者
 *
 * @author Ray
 * @since 2024-01-20
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IndexUpdateProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送索引更新任务
     *
     * @param message 索引更新消息
     */
    public void sendIndexUpdateTask(IndexUpdateMessage message) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.INDEX_EXCHANGE,
                    RabbitMQConfig.INDEX_UPDATE_ROUTING_KEY,
                    message);
            log.info("发送索引更新任务: action={}, type={}, id={}",
                    message.getAction(), message.getDocumentType(), message.getDocumentId());
        } catch (Exception e) {
            log.error("发送索引更新任务失败", e);
            throw new RuntimeException("发送消息失败: " + e.getMessage());
        }
    }

    /**
     * 创建索引
     */
    public void sendCreateIndex(String documentType, Long documentId, Long userId,
            String title, String content) {
        IndexUpdateMessage message = new IndexUpdateMessage(
                "CREATE", documentType, documentId, userId, title, content);
        sendIndexUpdateTask(message);
    }

    /**
     * 更新索引
     */
    public void sendUpdateIndex(String documentType, Long documentId, Long userId,
            String title, String content) {
        IndexUpdateMessage message = new IndexUpdateMessage(
                "UPDATE", documentType, documentId, userId, title, content);
        sendIndexUpdateTask(message);
    }

    /**
     * 删除索引
     */
    public void sendDeleteIndex(String documentType, Long documentId, Long userId) {
        IndexUpdateMessage message = new IndexUpdateMessage(
                "DELETE", documentType, documentId, userId, null, null);
        sendIndexUpdateTask(message);
    }
}
