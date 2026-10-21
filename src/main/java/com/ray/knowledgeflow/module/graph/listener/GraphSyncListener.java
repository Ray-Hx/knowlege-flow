package com.ray.knowledgeflow.module.graph.listener;

import com.ray.knowledgeflow.module.graph.service.IGraphService;
import com.ray.knowledgeflow.module.note.entity.NoteEntry;
import com.ray.knowledgeflow.module.note.service.NoteLinkService;
import com.ray.knowledgeflow.module.search.event.NoteDeletedEvent;
import com.ray.knowledgeflow.module.search.event.NoteSavedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * 知识图谱同步监听器
 *
 * @author Ray
 * @since 2024-01-16
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GraphSyncListener {

    private final IGraphService graphService;
    private final NoteLinkService noteLinkService;

    /**
     * 监听笔记保存事件（新建或更新）
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleNoteSaved(NoteSavedEvent event) {
        NoteEntry note = event.getNote();
        try {
            log.info("接收到笔记保存事件，准备同步图谱: id={}", note.getId());

            // 事务已提交，直接从 MySQL 事实表读取稳定关系。
            List<Long> linkedIds = noteLinkService.getOutgoingTargetIds(note.getUserId(), note.getId());

            // 同步到 Neo4j
            graphService.syncNote(note, linkedIds);

        } catch (Exception e) {
            log.error("同步图谱失败: noteId={}", note.getId(), e);
        }
    }

    /**
     * 监听笔记删除事件
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleNoteDeleted(NoteDeletedEvent event) {
        Long noteId = event.getNoteId();
        try {
            log.info("接收到笔记删除事件，准备同步图谱: id={}", noteId);
            graphService.deleteNote(noteId);
        } catch (Exception e) {
            log.error("从图谱删除节点失败: noteId={}", noteId, e);
        }
    }
}
