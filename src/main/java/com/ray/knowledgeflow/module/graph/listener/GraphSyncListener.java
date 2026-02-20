package com.ray.knowledgeflow.module.graph.listener;

import com.ray.knowledgeflow.module.graph.service.IGraphService;
import com.ray.knowledgeflow.module.note.dto.NoteDetailDto;
import com.ray.knowledgeflow.module.note.dto.NoteLinkDto;
import com.ray.knowledgeflow.module.note.entity.NoteEntry;
import com.ray.knowledgeflow.module.note.service.INoteService;
import com.ray.knowledgeflow.module.search.event.NoteDeletedEvent;
import com.ray.knowledgeflow.module.search.event.NoteSavedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

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
    private final INoteService noteService;

    /**
     * 监听笔记保存事件（新建或更新）
     */
    @Async
    @EventListener
    public void handleNoteSaved(NoteSavedEvent event) {
        NoteEntry note = event.getNote();
        try {
            log.info("接收到笔记保存事件，准备同步图谱: id={}", note.getId());

            // 调用 NoteService 获取详情（包含解析好的链接）
            NoteDetailDto detail = noteService.getNoteDetail(note.getUserId(), note.getId());

            // 提取出链ID列表
            List<Long> linkedIds = detail.getOutgoingLinks().stream()
                    .map(NoteLinkDto::getId)
                    .collect(Collectors.toList());

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
    @EventListener
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
