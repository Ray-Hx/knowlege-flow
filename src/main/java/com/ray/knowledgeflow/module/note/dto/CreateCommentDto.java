package com.ray.knowledgeflow.module.note.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建评论请求 DTO
 *
 * @author Ray
 * @since 2026-02-20
 */
@Data
public class CreateCommentDto {

    /**
     * 评论内容
     */
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容不能超过1000字")
    private String content;

    /**
     * 父评论ID（回复时填写，顶层评论留空）
     */
    private Long parentId;
}
