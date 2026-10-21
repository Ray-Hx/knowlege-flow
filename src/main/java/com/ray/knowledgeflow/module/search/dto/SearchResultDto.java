package com.ray.knowledgeflow.module.search.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 全局搜索结果 DTO
 *
 * @author Ray
 * @since 2024-03-XX
 */
@Data
public class SearchResultDto {
    private String id;
    private String type; // "NOTE" 或 "CONTENT"
    private String title; // 带高亮的标题
    private String snippet; // 带高亮的正文片段
    private LocalDateTime updateTime;
}
