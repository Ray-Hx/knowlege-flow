package com.ray.knowledgeflow.module.spaced.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 待复习笔记 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewNoteDto {

    /**
     * 笔记ID
     */
    private Long noteId;

    /**
     * 笔记标题
     */
    private String title;

    /**
     * 笔记摘要
     */
    private String summary;

    /**
     * 复习次数
     */
    private Integer repetitionCount;

    /**
     * 下次复习时间
     */
    private LocalDateTime nextReviewTime;

    /**
     * AI 生成的问题列表
     */
    private List<QuestionDto> questions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionDto {
        private Long id;
        private String question;
        private String answer;
    }
}
