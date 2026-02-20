package com.ray.knowledgeflow.module.user.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ray.knowledgeflow.common.exception.BusinessException;
import com.ray.knowledgeflow.module.user.dto.UserSettingsDto;
import com.ray.knowledgeflow.module.user.entity.SysUserProfile;
import com.ray.knowledgeflow.module.user.mapper.UserProfileMapper;
import com.ray.knowledgeflow.module.user.service.IUserSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户设置服务实现类
 *
 * @author Ray
 * @since 2024-01-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSettingsServiceImpl implements IUserSettingsService {

    private final UserProfileMapper userProfileMapper;
    private final ObjectMapper objectMapper;

    @Override
    public UserSettingsDto getSettings(Long userId) {
        SysUserProfile profile = userProfileMapper.selectById(userId);

        if (profile == null || profile.getPreferences() == null) {
            // 返回默认设置
            return createDefaultSettings();
        }

        try {
            return objectMapper.readValue(profile.getPreferences(), UserSettingsDto.class);
        } catch (JsonProcessingException e) {
            log.error("解析用户设置失败: userId={}", userId, e);
            return createDefaultSettings();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserSettingsDto updateSettings(Long userId, UserSettingsDto settings) {
        SysUserProfile profile = userProfileMapper.selectById(userId);

        if (profile == null) {
            profile = new SysUserProfile();
            profile.setUserId(userId);
            userProfileMapper.insert(profile);
        }

        try {
            String preferencesJson = objectMapper.writeValueAsString(settings);
            profile.setPreferences(preferencesJson);
            userProfileMapper.updateById(profile);
        } catch (JsonProcessingException e) {
            log.error("序列化用户设置失败", e);
            throw new BusinessException(500, "保存设置失败");
        }

        return settings;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserSettingsDto resetSettings(Long userId) {
        UserSettingsDto defaultSettings = createDefaultSettings();
        return updateSettings(userId, defaultSettings);
    }

    /**
     * 创建默认设置
     */
    private UserSettingsDto createDefaultSettings() {
        UserSettingsDto settings = new UserSettingsDto();
        settings.setUiSettings(new UserSettingsDto.UiSettings());
        settings.setNotificationSettings(new UserSettingsDto.NotificationSettings());
        settings.setLearningSettings(new UserSettingsDto.LearningSettings());
        settings.setPrivacySettings(new UserSettingsDto.PrivacySettings());
        return settings;
    }
}
