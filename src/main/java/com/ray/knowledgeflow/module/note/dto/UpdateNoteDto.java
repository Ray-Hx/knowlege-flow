package com.ray.knowledgeflow.module.note.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新笔记请求DTO
 *
 * @author Ray
 * @since 2024-01-16
 */
@Data
public class UpdateNoteDto {

    /**
     * 笔记标题
     */
    @Size(max = 255, message = "标题长度不能超过255个字符")
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
     * 类型
     */
    @Pattern(regexp = "NOTE|ARTICLE|SNIPPET", message = "类型必须是 NOTE、ARTICLE 或 SNIPPET")
    private String type;

    /**
     * 状态
     */
    @Min(value = 1, message = "状态值无效")
    @Max(value = 2, message = "状态值无效")
    private Integer status;

    /**
     * 摘要
     */
    private String summary;

    /**
     * 标签
     */
    /**
     * 标签
     */
    private String tags;

    /**
     * 是否公开: 0-否, 1-是
     */
    @Min(value = 0, message = "公开状态值无效")
    @Max(value = 1, message = "公开状态值无效")
    private Integer isPublic;
}
