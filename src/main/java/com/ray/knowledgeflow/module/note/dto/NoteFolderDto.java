package com.ray.knowledgeflow.module.note.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 笔记文件夹DTO
 *
 * @author Ray
 * @since 2024-01-16
 */
@Data
public class NoteFolderDto {

    /**
     * 文件夹ID
     */
    private Long id;

    /**
     * 文件夹名称
     */
    private String name;

    /**
     * 父文件夹ID
     */
    private Long parentId;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 子文件夹数量
     */
    private Integer subFolderCount;

    /**
     * 笔记数量
     */
    private Integer noteCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
}
