package com.ray.knowledgeflow.module.dashboard.controller;

import com.ray.knowledgeflow.common.result.Result;
import com.ray.knowledgeflow.module.dashboard.dto.DailyStudyDto;
import com.ray.knowledgeflow.module.dashboard.dto.DashboardStatsDto;
import com.ray.knowledgeflow.module.dashboard.dto.RecentNoteDto;
import com.ray.knowledgeflow.module.dashboard.service.IDashboardService;
import com.ray.knowledgeflow.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import com.ray.knowledgeflow.module.dashboard.dto.TagStatsDto;

import java.util.List;

/**
 * 仪表盘控制器
 */
@Tag(name = "仪表盘")
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final IDashboardService dashboardService;

    @GetMapping("/stats")
    @Operation(summary = "获取仪表盘统计数据")
    public Result<DashboardStatsDto> getStats() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(dashboardService.getStats(userId));
    }

    @GetMapping("/weekly")
    @Operation(summary = "获取本周每日学习数据")
    public Result<List<DailyStudyDto>> getWeeklyStudy() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(dashboardService.getWeeklyStudy(userId));
    }

    @GetMapping("/calendar")
    @Operation(summary = "获取月度日历数据")
    public Result<List<DailyStudyDto>> getMonthlyCalendar(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        Long userId = SecurityUtils.getCurrentUserId();

        // 默认当前月
        if (year == null)
            year = LocalDate.now().getYear();
        if (month == null)
            month = LocalDate.now().getMonthValue();

        return Result.success(dashboardService.getMonthlyCalendar(userId, year, month));
    }

    @GetMapping("/recent-notes")
    @Operation(summary = "获取最近编辑的笔记")
    public Result<List<RecentNoteDto>> getRecentNotes(
            @RequestParam(defaultValue = "5") Integer limit) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(dashboardService.getRecentNotes(userId, limit));
    }

    @GetMapping("/yearly-heatmap")
    @Operation(summary = "获取全年热力图数据（过去365天）")
    public Result<List<DailyStudyDto>> getYearlyHeatmap() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(dashboardService.getYearlyHeatmap(userId));
    }

    @GetMapping("/tag-distribution")
    @Operation(summary = "获取标签分布（Top 10）")
    public Result<List<TagStatsDto>> getTagDistribution() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(dashboardService.getTagDistribution(userId));
    }

    @GetMapping("/learning-trend")
    @Operation(summary = "获取近12周学习趋势")
    public Result<List<DailyStudyDto>> getLearningTrend() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(dashboardService.getLearningTrend(userId));
    }

    @PostMapping("/tags/sync")
    @Operation(summary = "同步AI标签")
    public Result<Void> syncTags() {
        Long userId = SecurityUtils.getCurrentUserId();
        dashboardService.syncAiTags(userId);
        return Result.success();
    }
}
