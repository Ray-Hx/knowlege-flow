package com.ray.knowledgeflow.module.note.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 创建笔记请求DTO
 *
 * @author Ray
 * @since 2024-01-16
 */
@Data
public class CreateNoteDto {

    /**
     * 笔记标题
     */
    @NotBlank(message = "笔记标题不能为空")
    private String title;

    /**
     * Markdown内容
     */
    private String contentMd;

    /**
     * 文件夹ID
     */
    private Long folderId;

    /**
     * 类型: NOTE(笔记), ARTICLE(文章), SNIPPET(片段)
     */
    @Pattern(regexp = "NOTE|ARTICLE|SNIPPET", message = "类型必须是 NOTE、ARTICLE 或 SNIPPET")
    private String type;

    /**
     * 状态: 1-草稿, 2-发布
     */
    @Min(value = 1, message = "状态值无效")
    @Max(value = 2, message = "状态值无效")
    private Integer status;

    /**
     * 是否公开: 0-否, 1-是
     */
    @Min(value = 0, message = "公开状态值无效")
    @Max(value = 1, message = "公开状态值无效")
    private Integer isPublic;
}
