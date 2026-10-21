package com.ray.knowledgeflow.module.note.message;

import com.ray.knowledgeflow.config.RabbitMQConfig;
import com.ray.knowledgeflow.module.note.service.NoteEngagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 互动计数同步消费者。
 *
 * <p>以数据库为准重算点赞数并覆盖 Redis。消息只带 noteId，因此重复消费或乱序到达
 * 都会收敛到同一结果，无需额外的幂等表。
 *
 * @author Ray
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EngagementSyncConsumer {

    private final NoteEngagementService engagementService;

    @RabbitListener(queues = RabbitMQConfig.ENGAGEMENT_SYNC_QUEUE)
    public void handleEngagementSync(EngagementSyncMessage message) {
        if (message == null || message.getNoteId() == null) {
            log.warn("忽略无效的互动计数同步消息: {}", message);
            return;
        }

        // 异常向上抛出，触发重试；重试耗尽后进入死信队列
        engagementService.refreshLikeCountFromDb(message.getNoteId(), message.getUserId());
        log.debug("互动计数已同步: noteId={}, action={}", message.getNoteId(), message.getAction());
    }
}
