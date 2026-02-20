package com.ray.knowledgeflow.module.user.dto;

import lombok.Data;

/**
 * 用户设置 DTO
 *
 * @author Ray
 * @since 2024-01-16
 */
@Data
public class UserSettingsDto {

    /**
     * UI 设置
     */
    private UiSettings uiSettings;

    /**
     * 通知设置
     */
    private NotificationSettings notificationSettings;

    /**
     * 学习设置
     */
    private LearningSettings learningSettings;

    /**
     * 隐私设置
     */
    private PrivacySettings privacySettings;

    /**
     * UI 设置
     */
    @Data
    public static class UiSettings {
        /**
         * 主题: light, dark, auto
         */
        private String theme = "light";

        /**
         * 语言: zh-CN, en-US
         */
        private String language = "zh-CN";

        /**
         * 侧边栏是否折叠
         */
        private Boolean sidebarCollapsed = false;

        /**
         * 字体大小: small, medium, large
         */
        private String fontSize = "medium";
    }

    /**
     * 通知设置
     */
    @Data
    public static class NotificationSettings {
        /**
         * 邮件通知
         */
        private Boolean emailEnabled = true;

        /**
         * 浏览器通知
         */
        private Boolean browserEnabled = true;

        /**
         * 每日复习提醒
         */
        private Boolean dailyReviewReminder = true;

        /**
         * 学习计划提醒
         */
        private Boolean learningPlanReminder = true;

        /**
         * 新内容提醒
         */
        private Boolean newContentAlert = false;
    }

    /**
     * 学习设置
     */
    @Data
    public static class LearningSettings {
        /**
         * 每日学习目标 (分钟)
         */
        private Integer dailyGoalMinutes = 60;

        /**
         * 番茄钟时长 (分钟)
         */
        private Integer pomodoroMinutes = 25;

        /**
         * 短休息时长 (分钟)
         */
        private Integer shortBreakMinutes = 5;

        /**
         * 长休息时长 (分钟)
         */
        private Integer longBreakMinutes = 15;

        /**
         * 每天复习卡片数量上限
         */
        private Integer dailyReviewLimit = 50;

        /**
         * 新卡片学习数量上限
         */
        private Integer dailyNewCardLimit = 20;
    }

    /**
     * 隐私设置
     */
    @Data
    public static class PrivacySettings {
        /**
         * 公开个人主页
         */
        private Boolean publicProfile = false;

        /**
         * 公开笔记
         */
        private Boolean publicNotes = false;

        /**
         * 公开学习统计
         */
        private Boolean publicStats = false;

        /**
         * 允许被搜索
         */
        private Boolean searchable = true;
    }
}
