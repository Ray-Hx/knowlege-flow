package com.ray.knowledgeflow.module.user.dto;

import lombok.Data;

/**
 * 用户资料 DTO
 *
 * @author Ray
 * @since 2024-01-16
 */
@Data
public class UserProfileDto {

    /**
     * 用户名 (只读)
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
     * 个人简介
     */
    private String bio;

    /**
     * GitHub ID
     */
    private String githubId;

    /**
     * 学习目标
     */
    private String learningGoals;
}
