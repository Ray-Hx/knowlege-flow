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
     * 获取用户的全量知识图谱
     *
     * @param userId 用户ID
     * @return 节点列表（包含关系）
     */
    List<NoteNode> getGraph(Long userId);

    /**
     * 获取全网公开笔记的知识图谱 (限制节点数)
     *
     * @param limit 最大返回的公开笔记数量
     * @return 节点列表（只包含关联这些节点的网络边界）
     */
    List<NoteNode> getGlobalGraph(int limit);

    /**
     * 使用 AI (向量相似度) 分析并建立笔记关联
     *
     * @param userId 用户ID
     * @return 建立的新关联数量
     */
    int analyzeRelations(Long userId);

    /**
     * 使用 AI 分析关联（流式进度回调）
     *
     * @param userId           用户ID
     * @param progressCallback 进度回调消息
     * @return 建立的新关联数量
     */
    int analyzeRelations(Long userId, java.util.function.Consumer<String> progressCallback);
}
