package com.ray.knowledgeflow.module.note.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 笔记详情DTO（含完整内容）
 *
 * @author Ray
 * @since 2024-01-16
 */
@Data
public class NoteDetailDto {

    /**
     * 笔记ID
     */
    private Long id;

    /**
     * 笔记标题
     */
    private String title;

    /**
     * Markdown内容
     */
    private String contentMd;

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
    private LocalDateTime updatedTime;

    /**
     * 引用的笔记列表（本笔记中引用了哪些笔记）
     */
    private List<NoteLinkDto> outgoingLinks;

    /**
     * 被引用列表（哪些笔记引用了本笔记）
     */
    private List<NoteLinkDto> incomingLinks;

    /**
     * 附件列表
     */
    private List<NoteAttachmentDto> attachments;

    /**
     * 引用的订阅内容列表（本笔记引用了哪些订阅文章）
     */
    /**
     * 引用的订阅内容列表（本笔记引用了哪些订阅文章）
     */
    private List<ContentLinkDto> contentLinks;

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
     * 是否公开: 0-否, 1-是
     */
    private Integer isPublic;

    /**
     * 浏览量
     */
    private Long viewCount;

    /**
     * 点赞数
     */
    private Long likeCount;

    /**
     * 当前用户是否已点赞
     */
    private Boolean isLiked;
}
