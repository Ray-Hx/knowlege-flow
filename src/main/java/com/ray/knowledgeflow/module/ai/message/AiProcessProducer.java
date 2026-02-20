package com.ray.knowledgeflow.module.ai.message;

import com.ray.knowledgeflow.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * AI 处理消息生产者
 *
 * @author Ray
 * @since 2024-01-20
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiProcessProducer {

    private final RabbitTemplate rabbitTemplate;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    /**
     * 发送 AI 处理任务到队列
     *
     * @param message AI 处理消息
     */
    public void sendAiProcessTask(AiProcessMessage message) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.AI_EXCHANGE,
                    RabbitMQConfig.AI_PROCESS_ROUTING_KEY,
                    message);
            log.info("发送AI处理任务: taskType={}, noteId={}",
                    message.getTaskType(), message.getNoteId());
        } catch (Exception e) {
            log.error("发送AI处理任务失败", e);
            throw new RuntimeException("发送消息失败: " + e.getMessage());
        }
    }

    /**
     * 发送笔记摘要生成任务（带去重）
     */
    public void sendSummaryTask(Long noteId, Long userId, String title, String content) {
        // 30 秒内同一笔记只发送一次摘要任务
        String dedupeKey = "msg:dedup:summary:" + noteId;
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(dedupeKey, "1", java.time.Duration.ofSeconds(30));

        if (Boolean.TRUE.equals(success)) {
            AiProcessMessage message = new AiProcessMessage(
                    "SUMMARY", noteId, userId, title, content);
            sendAiProcessTask(message);
        } else {
            log.debug("摘要任务去重: noteId={}", noteId);
        }
    }

    /**
     * 发送标签提取任务（带去重）
     */
    public void sendTagExtractionTask(Long noteId, Long userId, String title, String content) {
        String dedupeKey = "msg:dedup:tag:" + noteId;
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(dedupeKey, "1", java.time.Duration.ofSeconds(30));

        if (Boolean.TRUE.equals(success)) {
            AiProcessMessage message = new AiProcessMessage(
                    "TAG", noteId, userId, title, content);
            sendAiProcessTask(message);
        } else {
            log.debug("标签提取任务去重: noteId={}", noteId);
        }
    }

    /**
     * 发送图谱关系构建任务（延迟处理，不立即发送）
     */
    public void sendGraphBuildTask(Long noteId, Long userId, String title, String content) {
        // 标记为待处理，由定时任务批量处理
        redisTemplate.opsForSet().add("notes:pending:graph", noteId.toString());
        log.debug("图谱构建任务已标记为待处理: noteId={}", noteId);
    }
}
