package com.ray.knowledgeflow.module.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 推荐内容 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationDto {

    private Long id;

    private String title;

    private String summary;

    /**
     * 类型: NOTE 或 CONTENT
     */
    private String type;

    /**
     * 原始URL (仅 Content 类型有效)
     */
    private String url;

    /**
     * 推荐原因
     */
    private String reason;

    /**
     * 标签
     */
    private List<String> tags;

    private LocalDateTime updatedTime;
}
