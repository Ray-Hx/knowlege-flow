package com.ray.knowledgeflow.module.note.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 笔记附件DTO
 *
 * @author Ray
 * @since 2024-01-16
 */
@Data
public class NoteAttachmentDto {

    /**
     * 附件ID
     */
    private Long id;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 存储URL
     */
    private String url;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
}
