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
     * 发送索引更新任务。
     *
     * <p>尽力投递，失败只记日志不向上抛出：建立索引是对笔记的异步增强，
     * 而调用方（如 createNote）处于事务中，抛出会导致回滚——用户刚写的笔记
     * 会因为消息队列抖动而丢失，这个代价远大于索引延迟。
     *
     * <p>漏掉的消息可通过 POST /note/rebuild-index 重建补偿。
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
            // 日志需带全定位信息，便于事后核对哪些文档漏建了索引
            log.error("发送索引更新任务失败，该文档索引将缺失，可用 /note/rebuild-index 重建: "
                    + "action={}, type={}, id={}",
                    message.getAction(), message.getDocumentType(), message.getDocumentId(), e);
        }
    }

    /**
     * 创建索引
     */
    public void sendCreateIndex(String documentType, Long documentId, Long userId,
            String title, String content, String summary, String tags) {
        IndexUpdateMessage message = new IndexUpdateMessage(
                "CREATE", documentType, documentId, userId, title, content, summary, tags);
        sendIndexUpdateTask(message);
    }

    /**
     * 更新索引
     */
    public void sendUpdateIndex(String documentType, Long documentId, Long userId,
            String title, String content, String summary, String tags) {
        IndexUpdateMessage message = new IndexUpdateMessage(
                "UPDATE", documentType, documentId, userId, title, content, summary, tags);
        sendIndexUpdateTask(message);
    }

    /**
     * 删除索引
     */
    public void sendDeleteIndex(String documentType, Long documentId, Long userId) {
        IndexUpdateMessage message = new IndexUpdateMessage(
                "DELETE", documentType, documentId, userId, null, null, null, null);
        sendIndexUpdateTask(message);
    }
}
