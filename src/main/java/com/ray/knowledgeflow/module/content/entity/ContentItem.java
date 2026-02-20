package com.ray.knowledgeflow.module.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 聚合内容项实体类
 * </p>
 *
 * @author Ray
 * @since 2024-01-14
 */
@Data
@TableName("content_item")
public class ContentItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 来源ID
     */
    private Long sourceId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 内容标题
     */
    private String title;

    /**
     * 原始URL
     */
    private String url;

    /**
     * 作者
     */
    private String author;

    /**
     * 原始发布时间
     */
    private LocalDateTime publishTime;

    /**
     * AI生成的摘要
     */
    private String summary;

    /**
     * 提取的正文内容
     */
    private String contentText;

    /**
     * 提取的标签 (JSON)
     */
    private String tags;

    /**
     * 状态: 0-未读, 1-已读, 2-收藏, 3-归档
     */
    /**
     * 状态: 0-未读, 1-已读, 2-收藏, 3-归档, 4-重复
     */
    public static final int STATUS_UNREAD = 0;
    public static final int STATUS_READ = 1;
    public static final int STATUS_FAVORITE = 2;
    public static final int STATUS_ARCHIVED = 3;
    public static final int STATUS_DUPLICATE = 4;

    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;
}
