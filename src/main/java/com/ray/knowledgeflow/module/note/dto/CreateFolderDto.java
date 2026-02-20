package com.ray.knowledgeflow.module.note.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建文件夹请求DTO
 *
 * @author Ray
 * @since 2024-01-16
 */
@Data
public class CreateFolderDto {

    /**
     * 文件夹名称
     */
    @NotBlank(message = "文件夹名称不能为空")
    private String name;

    /**
     * 父文件夹ID（0表示根目录）
     */
    private Long parentId;

    /**
     * 排序
     */
    private Integer sortOrder;
}
