package com.ray.knowledgeflow.module.search.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 笔记删除事件
 *
 * @author Ray
 * @since 2024-01-16
 */
@Getter
public class NoteDeletedEvent extends ApplicationEvent {

    private final Long noteId;

    public NoteDeletedEvent(Object source, Long noteId) {
        super(source);
        this.noteId = noteId;
    }
}
