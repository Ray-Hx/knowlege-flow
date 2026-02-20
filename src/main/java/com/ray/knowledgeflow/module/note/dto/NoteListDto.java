package com.ray.knowledgeflow.module.note.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 笔记列表DTO（不含content_md）
 *
 * @author Ray
 * @since 2024-01-16
 */
@Data
public class NoteListDto {

    /**
     * 笔记ID
     */
    private Long id;

    /**
     * 笔记标题
     */
    private String title;

    /**
     * 摘要
     */
    private String summary;

    /**
     * 类型
     */
    private String type;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 文件夹ID
     */
    private Long folderId;

    /**
     * 文件夹名称
     */
    private String folderName;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;

    /**
     * 作者ID
     */
    private Long authorId;

    /**
     * 作者昵称
     */
    private String authorName;

    /**
     * 作者头像
     */
    private String authorAvatar;

    /**
     * 浏览量
     */
    private Long viewCount;

    /**
     * 点赞数
     */
    private Long likeCount;
}
