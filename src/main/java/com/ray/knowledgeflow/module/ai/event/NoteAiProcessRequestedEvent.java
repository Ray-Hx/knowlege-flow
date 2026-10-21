package com.ray.knowledgeflow.module.ai.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/** 笔记事务提交后需要执行的 AI 加工任务。 */
@Getter
public class NoteAiProcessRequestedEvent extends ApplicationEvent {

    private final Long noteId;
    private final Long userId;
    private final String title;
    private final String content;

    public NoteAiProcessRequestedEvent(Object source, Long noteId, Long userId, String title, String content) {
        super(source);
        this.noteId = noteId;
        this.userId = userId;
        this.title = title;
        this.content = content;
    }
}
