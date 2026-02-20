package com.ray.knowledgeflow.module.spaced.dto;

import lombok.Data;

/**
 * 间隔重复统计 DTO
 *
 * @author Ray
 * @since 2024-01-17
 */
@Data
public class SpacedStatsDto {
    /**
     * 总闪卡数
     */
    private Integer totalCards;

    /**
     * 今日到期数
     */
    private Integer dueToday;

    /**
     * 今日已复习数
     */
    private Integer reviewedToday;

    /**
     * 已掌握数 (重复次数 >= 3)
     */
    private Integer masteredCards;

    /**
     * 今日学习时长 (分钟)
     */
    private Integer totalMinutes;
}
