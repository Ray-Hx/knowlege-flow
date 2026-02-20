package com.ray.knowledgeflow.module.user.dto;

import lombok.Data;

/**
 * 公开用户信息 DTO
 *
 * @author Ray
 * @since 2024-01-17
 */
@Data
public class PublicUserProfileDto {

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称 (暂时复用username, 如果有nickname字段则使用)
     */
    private String nickname;

    /**
     * 头像 URL
     */
    private String avatar;

    /**
     * 个人简介
     */
    private String bio;

    /**
     * 学习目标
     */
    private String learningGoals;
}
