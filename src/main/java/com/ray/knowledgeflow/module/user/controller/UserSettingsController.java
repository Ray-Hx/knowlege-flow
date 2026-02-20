package com.ray.knowledgeflow.module.user.controller;

import com.ray.knowledgeflow.common.result.Result;
import com.ray.knowledgeflow.module.user.dto.UserSettingsDto;
import com.ray.knowledgeflow.module.user.service.IUserSettingsService;
import com.ray.knowledgeflow.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户设置控制器
 *
 * @author Ray
 * @since 2024-01-16
 */
@RestController
@RequestMapping("/user/settings")
@RequiredArgsConstructor
public class UserSettingsController {

    private final IUserSettingsService userSettingsService;

    /**
     * 获取用户设置
     */
    @GetMapping
    public Result<UserSettingsDto> getSettings() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(userSettingsService.getSettings(userId));
    }

    /**
     * 更新用户设置
     */
    @PutMapping
    public Result<UserSettingsDto> updateSettings(@RequestBody UserSettingsDto settings) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(userSettingsService.updateSettings(userId, settings));
    }

    /**
     * 重置用户设置为默认值
     */
    @PostMapping("/reset")
    public Result<UserSettingsDto> resetSettings() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(userSettingsService.resetSettings(userId));
    }

    /**
     * 更新 UI 设置
     */
    @PutMapping("/ui")
    public Result<UserSettingsDto> updateUiSettings(@RequestBody UserSettingsDto.UiSettings uiSettings) {
        Long userId = SecurityUtils.getCurrentUserId();
        UserSettingsDto settings = userSettingsService.getSettings(userId);
        settings.setUiSettings(uiSettings);
        return Result.success(userSettingsService.updateSettings(userId, settings));
    }

    /**
     * 更新通知设置
     */
    @PutMapping("/notification")
    public Result<UserSettingsDto> updateNotificationSettings(
            @RequestBody UserSettingsDto.NotificationSettings notificationSettings) {
        Long userId = SecurityUtils.getCurrentUserId();
        UserSettingsDto settings = userSettingsService.getSettings(userId);
        settings.setNotificationSettings(notificationSettings);
        return Result.success(userSettingsService.updateSettings(userId, settings));
    }

    /**
     * 更新学习设置
     */
    @PutMapping("/learning")
    public Result<UserSettingsDto> updateLearningSettings(
            @RequestBody UserSettingsDto.LearningSettings learningSettings) {
        Long userId = SecurityUtils.getCurrentUserId();
        UserSettingsDto settings = userSettingsService.getSettings(userId);
        settings.setLearningSettings(learningSettings);
        return Result.success(userSettingsService.updateSettings(userId, settings));
    }

    /**
     * 更新隐私设置
     */
    @PutMapping("/privacy")
    public Result<UserSettingsDto> updatePrivacySettings(
            @RequestBody UserSettingsDto.PrivacySettings privacySettings) {
        Long userId = SecurityUtils.getCurrentUserId();
        UserSettingsDto settings = userSettingsService.getSettings(userId);
        settings.setPrivacySettings(privacySettings);
        return Result.success(userSettingsService.updateSettings(userId, settings));
    }
}
