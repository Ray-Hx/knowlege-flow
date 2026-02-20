package com.ray.knowledgeflow.module.note.dto;

import lombok.Data;

/**
 * 内容链接DTO（用于笔记引用订阅内容）
 *
 * @author Ray
 * @since 2024-01-29
 */
@Data
public class ContentLinkDto {

    /**
     * 内容项ID
     */
    private Long id;

    /**
     * 内容标题
     */
    private String title;

    /**
     * 原始URL
     */
    private String url;

    /**
     * 来源名称
     */
    private String sourceName;

    /**
     * 摘要
     */
    private String summary;
}
