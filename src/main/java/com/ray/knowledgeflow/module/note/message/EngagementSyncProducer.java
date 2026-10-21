package com.ray.knowledgeflow.module.note.message;

import com.ray.knowledgeflow.config.RabbitMQConfig;
import com.ray.knowledgeflow.module.note.event.NoteLikeChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 互动计数同步消息生产者。
 *
 * @author Ray
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EngagementSyncProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 事务提交成功后才投递消息。
     *
     * <p>发送失败不向上抛出：此时数据库已提交，抛异常既回滚不了事务，
     * 又会让用户看到失败。缓存值由 TTL 到期后回源自愈，代价可接受。
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLikeChanged(NoteLikeChangedEvent event) {
        EngagementSyncMessage message = new EngagementSyncMessage(
                event.getNoteId(), event.getUserId(), event.getAction());
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ENGAGEMENT_EXCHANGE,
                    RabbitMQConfig.ENGAGEMENT_SYNC_ROUTING_KEY,
                    message);
            log.debug("发送互动计数同步消息: noteId={}, action={}", event.getNoteId(), event.getAction());
        } catch (Exception e) {
            log.error("发送互动计数同步消息失败，缓存将由 TTL 过期后自愈: noteId={}", event.getNoteId(), e);
        }
    }
}
