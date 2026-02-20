package com.ray.knowledgeflow.module.note.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 笔记评论 DTO（响应）
 *
 * @author Ray
 * @since 2026-02-20
 */
@Data
public class NoteCommentDto {

    private Long id;

    private Long noteId;

    private Long parentId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 评论者ID
     */
    private Long userId;

    /**
     * 评论者昵称
     */
    private String authorName;

    /**
     * 评论者头像
     */
    private String authorAvatar;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 子评论列表（仅顶层评论才会有，最多一层）
     */
    private List<NoteCommentDto> replies;
}
