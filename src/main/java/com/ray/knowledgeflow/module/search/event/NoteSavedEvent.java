package com.ray.knowledgeflow.module.search.event;

import com.ray.knowledgeflow.module.note.entity.NoteEntry;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 笔记保存事件（新建或更新）
 *
 * @author Ray
 * @since 2024-01-16
 */
@Getter
public class NoteSavedEvent extends ApplicationEvent {

    private final NoteEntry note;

    public NoteSavedEvent(Object source, NoteEntry note) {
        super(source);
        this.note = note;
    }
}
