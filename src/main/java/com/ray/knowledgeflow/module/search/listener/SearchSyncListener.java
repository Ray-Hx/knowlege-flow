package com.ray.knowledgeflow.module.search.listener;

import com.ray.knowledgeflow.module.search.event.NoteDeletedEvent;
import com.ray.knowledgeflow.module.search.event.NoteSavedEvent;
import com.ray.knowledgeflow.module.search.service.ISearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

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

    private final ISearchService searchService;

    @Async
    @EventListener
    public void handleNoteSaved(NoteSavedEvent event) {
        log.info("接收到笔记保存事件，开始同步索引: id={}", event.getNote().getId());
        searchService.syncNote(event.getNote());
    }

    @Async
    @EventListener
    public void handleNoteDeleted(NoteDeletedEvent event) {
        log.info("接收到笔记删除事件，开始删除索引: id={}", event.getNoteId());
        searchService.deleteNoteIndex(event.getNoteId());
    }

    @Async
    @EventListener
    public void handleContentSaved(com.ray.knowledgeflow.module.search.event.ContentSavedEvent event) {
        log.info("接收到内容保存事件，开始同步索引: id={}", event.getContent().getId());
        searchService.syncContent(event.getContent());
    }
}
