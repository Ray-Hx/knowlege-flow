package com.ray.knowledgeflow.module.graph.service;

import com.ray.knowledgeflow.module.graph.entity.NoteNode;
import com.ray.knowledgeflow.module.note.entity.NoteEntry;

import java.util.List;

/**
 * 知识图谱服务接口
 *
 * @author Ray
 * @since 2024-01-16
 */
public interface IGraphService {

    /**
     * 同步笔记到图谱
     *
     * @param note          笔记实体
     * @param linkedNoteIds 该笔记引用的所有笔记ID列表
     */
    void syncNote(NoteEntry note, List<Long> linkedNoteIds);

    /**
     * 从图谱中删除笔记
     *
     * @param noteId 笔记ID
     */
    void deleteNote(Long noteId);

    /**
     * 从 MySQL 笔记与 note_link 事实表重建用户的显式图投影。
     */
    int rebuildGraph(Long userId);

    /**
     * 获取用户的全量知识图谱
     *
     * @param userId 用户ID
     * @return 节点列表（包含关系）
     */
    List<NoteNode> getGraph(Long userId);

    /**
     * 使用 AI (向量相似度) 分析并建立笔记关联
     *
     * @param userId 用户ID
     * @return 建立的新关联数量
     */
    int analyzeRelations(Long userId);

    /**
     * 流式进行 AI 分析，将进度发送到 Consumer
     */
    int analyzeRelations(Long userId, java.util.function.Consumer<String> progressConsumer);

    /**
     * 获取社区层面的全景知识星光图谱
     *
     * @param limit 获取图谱网络中前N个最热门的公开笔记的节点信息
     * @return 图谱节点及关系
     */
    List<NoteNode> getCommunityGraph(int limit);
}
