package com.ray.knowledgeflow.module.content.listener;

import com.ray.knowledgeflow.module.content.entity.ContentItem;
import com.ray.knowledgeflow.module.content.service.ContentProcessorService;
import com.ray.knowledgeflow.module.search.event.ContentSavedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 内容处理监听器
 * 监听内容保存事件，异步执行内容处理
 *
 * @author Ray
 * @since 2024-01-17
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentProcessListener {

    private final ContentProcessorService contentProcessorService;

    /**
     * 处理内容保存事件
     * 执行去重检测、关键词提取、自动分类
     *
     * @param event 内容保存事件
     */
    @Async
    @EventListener
    public void handleContentSaved(ContentSavedEvent event) {
        ContentItem item = event.getContent();

        log.info("接收到内容保存事件，开始处理: id={}, title={}", item.getId(), item.getTitle());

        try {
            // 执行内容处理
            contentProcessorService.processContent(item);
        } catch (Exception e) {
            log.error("内容处理失败: id={}, title={}", item.getId(), item.getTitle(), e);
        }
    }
}
