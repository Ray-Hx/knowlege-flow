package com.ray.knowledgeflow.module.output.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户主页配置实体类
 *
 * @author Ray
 * @since 2024-01-26
 */
@Data
@TableName("user_homepage")
public class UserHomepage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 个性化URL（如：ray-huang）
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
     * 主题：default, dark, light
     */
    private String theme;

    /**
     * 布局：standard, compact, minimal
     */
    private String layout;

    /**
     * 是否显示统计：0-否，1-是
     */
    private Integer showStats;

    /**
     * 是否显示技能：0-否，1-是
     */
    private Integer showSkills;

    /**
     * 是否显示笔记：0-否，1-是
     */
    private Integer showNotes;

    /**
     * 自定义CSS
     */
    private String customCss;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;
}
