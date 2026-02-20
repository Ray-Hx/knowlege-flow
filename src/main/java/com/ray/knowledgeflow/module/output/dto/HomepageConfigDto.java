package com.ray.knowledgeflow.module.output.dto;

import lombok.Data;

/**
 * 主页配置DTO
 *
 * @author Ray
 * @since 2024-01-26
 */
@Data
public class HomepageConfigDto {

    /**
     * 个性化URL
     */
    private String slug;

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
    private Integer showStats;

    /**
     * 是否显示技能
     */
    private Integer showSkills;

    /**
     * 是否显示笔记
     */
    private Integer showNotes;

    /**
     * 自定义CSS
     */
    private String customCss;
}
