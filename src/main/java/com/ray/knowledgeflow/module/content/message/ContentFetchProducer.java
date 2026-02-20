package com.ray.knowledgeflow.module.content.message;

import com.ray.knowledgeflow.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 内容抓取消息生产者
 *
 * @author Ray
 * @since 2024-01-20
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentFetchProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送内容抓取任务到队列
     *
     * @param message 抓取任务消息
     */
    public void sendFetchTask(ContentFetchMessage message) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CONTENT_EXCHANGE,
                    RabbitMQConfig.CONTENT_FETCH_ROUTING_KEY,
                    message);
            log.info("发送内容抓取任务: type={}, sourceId={}, userId={}",
                    message.getType(), message.getSourceId(), message.getUserId());
        } catch (Exception e) {
            log.error("发送内容抓取任务失败", e);
            throw new RuntimeException("发送消息失败: " + e.getMessage());
        }
    }
}
