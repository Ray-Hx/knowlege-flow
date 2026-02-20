package com.ray.knowledgeflow.module.spaced.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 复习结果 DTO
 *
 * @author Ray
 * @since 2024-01-17
 */
@Data
public class ReviewResultDto {
    /**
     * 质量评分 (0-5)
     * 5: 完美记忆
     * 4: 犹豫后正确
     * 3: 困难但正确
     * 2: 错误但有印象
     * 1: 错误且印象模糊
     * 0: 完全没印象
     */
    @NotNull(message = "质量评分不能为空")
    @Min(value = 0, message = "评分最小为0")
    @Max(value = 5, message = "评分最大为5")
    private Integer quality;

    /**
     * 耗时 (毫秒)
     */
    private Integer durationMs;
}
