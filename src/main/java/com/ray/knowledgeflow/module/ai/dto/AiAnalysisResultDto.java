package com.ray.knowledgeflow.module.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 深度解析结果 DTO
 *
 * @author Ray
 * @since 2024-01-27
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiAnalysisResultDto {
    private String content;
    private LocalDateTime generatedTime;
}
