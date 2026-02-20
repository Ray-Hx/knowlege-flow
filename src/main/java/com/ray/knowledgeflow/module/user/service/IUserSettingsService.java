package com.ray.knowledgeflow.module.user.service;

import com.ray.knowledgeflow.module.user.dto.UserSettingsDto;

/**
 * 用户设置服务接口
 *
 * @author Ray
 * @since 2024-01-16
 */
public interface IUserSettingsService {

    /**
     * 获取用户设置
     *
     * @param userId 用户ID
     * @return 用户设置
     */
    UserSettingsDto getSettings(Long userId);

    /**
     * 更新用户设置
     *
     * @param userId   用户ID
     * @param settings 设置
     * @return 更新后的设置
     */
    UserSettingsDto updateSettings(Long userId, UserSettingsDto settings);

    /**
     * 重置用户设置为默认值
     *
     * @param userId 用户ID
     * @return 默认设置
     */
    UserSettingsDto resetSettings(Long userId);
}
