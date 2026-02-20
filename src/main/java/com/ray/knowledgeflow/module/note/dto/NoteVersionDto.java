package com.ray.knowledgeflow.module.note.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 笔记版本DTO
 *
 * @author Ray
 * @since 2024-01-16
 */
@Data
public class NoteVersionDto {

    /**
     * 版本ID
     */
    private Long id;

    /**
     * 笔记ID
     */
    private Long noteId;

    /**
     * 标题快照
     */
    private String title;

    /**
     * 内容快照
     */
    private String contentMd;

    /**
     * 版本时间
     */
    private LocalDateTime createdTime;
}
