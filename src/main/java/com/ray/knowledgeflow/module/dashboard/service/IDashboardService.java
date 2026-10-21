package com.ray.knowledgeflow.module.dashboard.service;

import com.ray.knowledgeflow.module.dashboard.dto.DailyStudyDto;
import com.ray.knowledgeflow.module.dashboard.dto.DashboardStatsDto;
import com.ray.knowledgeflow.module.dashboard.dto.RecentNoteDto;
import com.ray.knowledgeflow.module.dashboard.dto.TagStatsDto;

import java.util.List;

/**
 * 仪表盘服务接口
 */
public interface IDashboardService {

    /**
     * 获取仪表盘统计数据
     */
    DashboardStatsDto getStats(Long userId);

    /**
     * 获取本周每日学习数据
     */
    List<DailyStudyDto> getWeeklyStudy(Long userId);

    /**
     * 获取月度日历数据 (用于热力图)
     */
    List<DailyStudyDto> getMonthlyCalendar(Long userId, int year, int month);

    /**
     * 获取最近编辑的笔记
     */
    List<RecentNoteDto> getRecentNotes(Long userId, int limit);

    /**
     * 获取过去365天的每日笔记数（GitHub风格热力图）
     */
    List<DailyStudyDto> getYearlyHeatmap(Long userId);

    /**
     * 获取标签分布统计（Top 10）
     */
    List<TagStatsDto> getTagDistribution(Long userId);

    /**
     * 获取近12周每周笔记数（学习趋势折线图）
     */
    List<DailyStudyDto> getLearningTrend(Long userId);

    /**
     * 同步尚未打标签的笔记，通过AI提取标签
     */
    void syncAiTags(Long userId);
}
