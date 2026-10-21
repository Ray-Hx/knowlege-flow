package com.ray.knowledgeflow.module.search.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 内容删除事件
 */
@Getter
public class ContentDeletedEvent extends ApplicationEvent {
    private final Long contentId;

    public ContentDeletedEvent(Object source, Long contentId) {
        super(source);
        this.contentId = contentId;
    }
}
