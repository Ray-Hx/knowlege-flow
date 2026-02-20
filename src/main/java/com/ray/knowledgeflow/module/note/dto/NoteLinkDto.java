package com.ray.knowledgeflow.module.note.dto;

import lombok.Data;

/**
 * 笔记链接DTO（用于双向链接）
 *
 * @author Ray
 * @since 2024-01-16
 */
@Data
public class NoteLinkDto {

    /**
     * 笔记ID
     */
    private Long id;

    /**
     * 笔记标题
     */
    private String title;

    /**
     * 笔记摘要
     */
    private String summary;
}
