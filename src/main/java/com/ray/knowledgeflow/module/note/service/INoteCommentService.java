package com.ray.knowledgeflow.module.note.service;

import com.ray.knowledgeflow.module.note.dto.CreateCommentDto;
import com.ray.knowledgeflow.module.note.dto.NoteCommentDto;

import java.util.List;

/**
 * 笔记评论服务接口
 *
 * @author Ray
 * @since 2026-02-20
 */
public interface INoteCommentService {

    /**
     * 获取笔记的评论列表（顶层 + 一层回复）
     *
     * @param noteId 笔记ID
     * @return 评论列表
     */
    List<NoteCommentDto> getComments(Long noteId);

    /**
     * 发表评论
     *
     * @param userId 用户ID
     * @param noteId 笔记ID
     * @param dto    创建请求
     * @return 新增的评论
     */
    NoteCommentDto addComment(Long userId, Long noteId, CreateCommentDto dto);

    /**
     * 删除评论（仅评论作者可操作）
     *
     * @param userId    当前用户ID
     * @param commentId 评论ID
     */
    void deleteComment(Long userId, Long commentId);

    /**
     * 获取笔记的评论总数
     *
     * @param noteId 笔记ID
     * @return 评论数
     */
    long getCommentCount(Long noteId);
}
