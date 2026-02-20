package com.ray.knowledgeflow.module.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 笔记引用DTO
 *
 * @author Ray
 * @since 2024-01-17
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteReferenceDto {
    private Long noteId;
    private String title;
    private String snippet; // 相关片段
    private Double score; // 相似度分数
}
