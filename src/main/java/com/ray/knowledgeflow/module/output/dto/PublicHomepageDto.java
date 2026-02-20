package com.ray.knowledgeflow.module.output.dto;

import lombok.Data;

import java.util.List;

/**
 * 公开主页DTO
 *
 * @author Ray
 * @since 2024-01-26
 */
@Data
public class PublicHomepageDto {

    /**
     * 用户名
     */
    private String username;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 主页标题
     */
    private String title;

    /**
     * 个人简介
     */
    private String bio;

    /**
     * 主题
     */
    private String theme;

    /**
     * 布局
     */
    private String layout;

    /**
     * 是否显示统计
     */
    private Boolean showStats;

    /**
     * 是否显示技能
     */
    private Boolean showSkills;

    /**
     * 是否显示笔记
     */
    private Boolean showNotes;

    /**
     * 自定义CSS
     */
    private String customCss;

    /**
     * 学习统计（如果显示）
     */
    private LearningStatsDto stats;

    /**
     * 技能列表（如果显示）
     */
    private List<String> skills;

    /**
     * 公开笔记数量（如果显示）
     */
    private Integer publicNoteCount;

    @Data
    public static class LearningStatsDto {
        /**
         * 总学习时长（分钟）
         */
        private Long totalMinutes;

        /**
         * 笔记数量
         */
        private Integer noteCount;

        /**
         * 连续学习天数
         */
        private Integer streakDays;
    }
}
