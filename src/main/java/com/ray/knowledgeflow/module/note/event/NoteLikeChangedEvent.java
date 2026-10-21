package com.ray.knowledgeflow.module.note.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 点赞状态变更事件。
 *
 * <p>在事务内发布，由 {@code @TransactionalEventListener(AFTER_COMMIT)} 监听后再投递 MQ。
 * 这样数据库回滚时不会有消息发出，从根本上避免"库里没写成、缓存却改了"的双写不一致。
 *
 * @author Ray
 */
@Getter
public class NoteLikeChangedEvent extends ApplicationEvent {

    private final Long noteId;
    private final Long userId;
    private final String action;

    public NoteLikeChangedEvent(Object source, Long noteId, Long userId, String action) {
        super(source);
        this.noteId = noteId;
        this.userId = userId;
        this.action = action;
    }
}
