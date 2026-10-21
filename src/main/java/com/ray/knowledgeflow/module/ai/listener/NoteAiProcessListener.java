package com.ray.knowledgeflow.module.ai.listener;

import com.ray.knowledgeflow.module.ai.event.NoteAiProcessRequestedEvent;
import com.ray.knowledgeflow.module.ai.message.AiProcessProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 仅在笔记事务成功提交后投递 AI 摘要与标签任务。 */
@Component
@RequiredArgsConstructor
public class NoteAiProcessListener {

    private final AiProcessProducer producer;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handle(NoteAiProcessRequestedEvent event) {
        producer.sendSummaryTask(event.getNoteId(), event.getUserId(), event.getTitle(), event.getContent());
        producer.sendTagExtractionTask(event.getNoteId(), event.getUserId(), event.getTitle(), event.getContent());
    }
}
