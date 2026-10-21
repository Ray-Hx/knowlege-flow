package com.ray.knowledgeflow.module.search.listener;

import com.ray.knowledgeflow.module.search.event.NoteDeletedEvent;
import com.ray.knowledgeflow.module.search.event.NoteSavedEvent;
import com.ray.knowledgeflow.module.search.event.ContentDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 搜索同步监听器
 *
 * @author Ray
 * @since 2024-01-16
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchSyncListener {

    private final com.ray.knowledgeflow.module.search.message.IndexUpdateProducer indexUpdateProducer;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleNoteSaved(NoteSavedEvent event) {
        log.info("接收到笔记保存事件，准备投递索引任务: id={}", event.getNote().getId());
        indexUpdateProducer.sendUpdateIndex(
                "NOTE",
                event.getNote().getId(),
                event.getNote().getUserId(),
                event.getNote().getTitle(),
                event.getNote().getContentMd(),
                event.getNote().getSummary(),
                event.getNote().getTags());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleNoteDeleted(NoteDeletedEvent event) {
        log.info("接收到笔记删除事件，准备投递索引任务: id={}", event.getNoteId());
        indexUpdateProducer.sendDeleteIndex("NOTE", event.getNoteId(), null);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleContentSaved(com.ray.knowledgeflow.module.search.event.ContentSavedEvent event) {
        log.info("接收到内容保存事件，准备投递索引任务: id={}", event.getContent().getId());

        // 触发向量索引及 RAG 切片
        indexUpdateProducer.sendCreateIndex(
                "CONTENT_ITEM",
                event.getContent().getId(),
                event.getContent().getUserId(),
                event.getContent().getTitle(),
                event.getContent().getContentText(),
                event.getContent().getSummary(),
                event.getContent().getTags());
    }

    /**
     * 处理内容删除事件
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleContentDeleted(ContentDeletedEvent event) {
        log.info("收到内容删除事件: id={}", event.getContentId());
        // 发送异步删除消息
        indexUpdateProducer.sendDeleteIndex("CONTENT_ITEM", event.getContentId(), null);
    }
}
