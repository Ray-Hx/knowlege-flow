package com.ray.knowledgeflow.module.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息 DTO (完整)
 *
 * @author Ray
 * @since 2024-01-16
 */
@Data
public class UserInfoDto {

    /**
     * 用户 ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 头像 URL
     */
    private String avatar;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 个人简介
     */
    private String bio;

    /**
     * GitHub ID
     */
    private String githubId;

    /**
     * 用户偏好设置 (JSON)
     */
    private UserSettingsDto preferences;

    /**
     * 学习目标 (JSON)
     */
    /**
     * 学习目标 (JSON)
     */
    private String learningGoals;

    /**
     * 用户角色
     */
    private java.util.List<String> roles;
}
