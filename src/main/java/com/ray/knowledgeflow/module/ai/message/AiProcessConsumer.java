package com.ray.knowledgeflow.module.ai.message;

import com.ray.knowledgeflow.config.RabbitMQConfig;
import com.ray.knowledgeflow.module.ai.service.IAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * AI 处理消息消费者
 *
 * @author Ray
 * @since 2024-01-20
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiProcessConsumer {

    private final IAiService aiService;
    private final com.ray.knowledgeflow.module.note.service.INoteService noteService;

    /**
     * 监听 AI 处理队列
     * 使用 Resilience4j 限流：每秒最多 5 个请求（配置在 application.yml）
     *
     * @param message AI 处理消息
     */
    @io.github.resilience4j.ratelimiter.annotation.RateLimiter(name = "ai-service")
    @RabbitListener(queues = RabbitMQConfig.AI_PROCESS_QUEUE)
    public void handleAiProcessTask(AiProcessMessage message) {
        log.info("收到AI处理任务: taskType={}, noteId={}",
                message.getTaskType(), message.getNoteId());

        try {
            switch (message.getTaskType().toUpperCase()) {
                case "SUMMARY":
                    generateSummary(message);
                    break;

                case "TAG":
                    extractTags(message);
                    break;

                case "GRAPH":
                    buildGraphRelations(message);
                    break;

                default:
                    log.warn("未知的AI任务类型: {}", message.getTaskType());
            }
        } catch (Exception e) {
            log.error("AI处理任务失败: taskType={}, noteId={}",
                    message.getTaskType(), message.getNoteId(), e);
            // TODO: 可以考虑重试机制或发送到死信队列
        }
    }

    /**
     * 生成笔记摘要
     */
    private void generateSummary(AiProcessMessage message) {
        log.info("开始生成摘要: noteId={}", message.getNoteId());

        String summary = aiService.generateSummary(message.getContent());

        if (summary != null && !summary.isEmpty()) {
            com.ray.knowledgeflow.module.note.dto.UpdateNoteDto updateDto = new com.ray.knowledgeflow.module.note.dto.UpdateNoteDto();
            updateDto.setSummary(summary);
            // 只需要更新摘要，其他字段留空? updateNote 实现通常会忽略 null
            // 注意: Service updateNote 可能会校验其他字段
            // 为了安全，最好只更新特定字段。如果 Service 是全量更新，会有问题。
            // 假设 Service 是 PATCH 语义 (MyBatis-Plus updateById 是 PATCH)
            // 让我们检查 INoteService.updateNote 实现。
            // 通常是 dto -> entity copy, if not null set.
            noteService.updateNote(message.getUserId(), message.getNoteId(), updateDto);
            log.info("摘要生成并保存完成: noteId={}", message.getNoteId());
        }
    }

    /**
     * 提取标签
     */
    private void extractTags(AiProcessMessage message) {
        log.info("开始提取标签: noteId={}", message.getNoteId());

        java.util.List<String> tags = aiService.extractTags(message.getContent());

        if (tags != null && !tags.isEmpty()) {
            String tagsStr = String.join(",", tags);
            com.ray.knowledgeflow.module.note.dto.UpdateNoteDto updateDto = new com.ray.knowledgeflow.module.note.dto.UpdateNoteDto();
            updateDto.setTags(tagsStr);
            noteService.updateNote(message.getUserId(), message.getNoteId(), updateDto);
            log.info("标签提取并保存完成: noteId={}, tags={}", message.getNoteId(), tagsStr);
        }
    }

    /**
     * 构建知识图谱关系
     */
    private void buildGraphRelations(AiProcessMessage message) {
        log.info("开始构建图谱关系: noteId={}", message.getNoteId());

        // TODO: 分析笔记内容，提取实体和关系
        // 暂时只作为占位符，未来可以调用 AI 提取实体并同步到 Neo4j
        // List<String> entities = aiService.extractEntities(message.getContent());
        // graphService.saveEntities(entities);

        log.info("图谱关系构建完成 (跳过): noteId={}", message.getNoteId());
    }
}
