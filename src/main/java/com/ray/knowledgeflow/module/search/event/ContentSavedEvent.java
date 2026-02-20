package com.ray.knowledgeflow.module.search.event;

import com.ray.knowledgeflow.module.content.entity.ContentItem;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 聚合内容保存事件
 *
 * @author Ray
 * @since 2024-01-16
 */
@Getter
public class ContentSavedEvent extends ApplicationEvent {

    private final ContentItem content;

    public ContentSavedEvent(Object source, ContentItem content) {
        super(source);
        this.content = content;
    }
}
