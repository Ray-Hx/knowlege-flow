package com.ray.knowledgeflow.module.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 仪表盘统计数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {

    /**
     * 总笔记数
     */
    private Long totalNotes;

    /**
     * 待复习笔记数
     */
    private Long pendingNotes;

    /**
     * 本周新增笔记数
     */
    private Long weeklyNewNotes;

    /**
     * 今日新增笔记数
     */
    private Long todayNewNotes;
}
