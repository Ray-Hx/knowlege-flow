package com.ray.knowledgeflow.module.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新用户资料请求 DTO
 *
 * @author Ray
 * @since 2024-01-16
 */
@Data
public class UpdateProfileDto {

    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 个人简介
     */
    @Size(max = 500, message = "个人简介不能超过500个字符")
    private String bio;

    /**
     * GitHub ID
     */
    @Size(max = 64, message = "GitHub ID不能超过64个字符")
    private String githubId;

    /**
     * 用户偏好设置 (JSON)
     */
    private String preferences;

    /**
     * 学习目标 (JSON)
     */
    private String learningGoals;
}
