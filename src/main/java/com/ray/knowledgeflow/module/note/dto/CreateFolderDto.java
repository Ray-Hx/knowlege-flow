package com.ray.knowledgeflow.module.note.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

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
    @Size(max = 100, message = "文件夹名称不能超过100个字符")
    private String name;

    /**
     * 父文件夹ID（0表示根目录）
     */
    @Min(value = 0, message = "父文件夹ID不合法")
    private Long parentId;

    /**
     * 排序
     */
    private Integer sortOrder;
}
