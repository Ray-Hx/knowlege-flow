package com.ray.knowledgeflow.module.content.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 内容项 DTO
 *
 * @author Ray
 * @since 2024-01-16
 */
@Data
public class ContentItemDto {

    /**
     * ID
     */
    private Long id;

    /**
     * 来源 ID
     */
    private Long sourceId;

    /**
     * 来源名称
     */
    private String sourceName;

    /**
     * 标题
     */
    private String title;

    /**
     * 原始 URL
     */
    private String url;

    /**
     * 作者
     */
    private String author;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;

    /**
     * 摘要
     */
    private String summary;

    /**
     * 状态: 0-未读, 1-已读, 2-收藏, 3-归档
     */
    private Integer status;

    /**
     * 正文内容
     */
    private String contentText;

    /**
     * 标签
     */
    private String tags;

    /**
     * 入库时间
     */
    private LocalDateTime createdTime;
}
