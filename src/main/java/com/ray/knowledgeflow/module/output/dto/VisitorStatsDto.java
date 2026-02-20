package com.ray.knowledgeflow.module.output.dto;

import lombok.Data;

/**
 * 访客统计DTO
 *
 * @author Ray
 * @since 2024-01-26
 */
@Data
public class VisitorStatsDto {

    /**
     * 今日访客数
     */
    private Integer todayVisitors;

    /**
     * 今日访问量
     */
    private Integer todayViews;

    /**
     * 总访客数
     */
    private Integer totalVisitors;

    /**
     * 总访问量
     */
    private Integer totalViews;

    /**
     * 最近7天访问量
     */
    private Integer last7DaysViews;

    /**
     * 最近30天访问量
     */
    private Integer last30DaysViews;
}
