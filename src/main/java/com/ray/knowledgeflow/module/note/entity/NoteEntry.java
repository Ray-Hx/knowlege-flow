package com.ray.knowledgeflow.module.note.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 笔记实体类
 * </p>
 *
 * @author Ray
 * @since 2024-01-14
 */
@Data
@TableName("note_entry")
public class NoteEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 文件夹ID
     */
    private Long folderId;

    /**
     * 笔记标题
     */
    private String title;

    /**
     * Markdown内容
     */
    private String contentMd;

    /**
     * 简短摘要
     */
    private String summary;

    /**
     * 标签，逗号分隔
     */
    private String tags;

    /**
     * 类型: NOTE(笔记), ARTICLE(文章), SNIPPET(片段)
     */
    private String type;

    /**
     * 状态: 1-草稿, 2-发布
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    /**
     * 是否公开: 0-否, 1-是
     */
    private Integer isPublic;

    /**
     * 累计浏览量。Redis 承担高频自增，由定时任务回写此列。
     */
    private Long viewCount;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;
}
