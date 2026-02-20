package com.ray.knowledgeflow.module.spaced.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 复习状态 DTO
 *
 * @author Ray
 * @since 2024-01-17
 */
@Data
public class ReviewStatusDto {
    /**
     * 下次复习时间
     */
    private LocalDateTime nextReviewTime;

    /**
     * 间隔天数
     */
    private Integer intervalDays;

    /**
     * 重复次数
     */
    private Integer repetitionCount;

    /**
     * 难度因子 (EF)
     */
    private BigDecimal easinessFactor;

    /**
     * 是否今日到期
     */
    private Boolean isDueToday;
}
