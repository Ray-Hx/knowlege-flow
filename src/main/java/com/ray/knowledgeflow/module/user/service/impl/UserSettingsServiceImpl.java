package com.ray.knowledgeflow.module.user.service.impl;

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

    @Override
    public UserSettingsDto getSettings(Long userId) {
        SysUserProfile profile = userProfileMapper.selectById(userId);

        if (profile == null || profile.getPreferences() == null) {
            // 返回默认设置
            return createDefaultSettings();
        }

        return profile.getPreferences();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserSettingsDto updateSettings(Long userId, UserSettingsDto settings) {
        SysUserProfile profile = userProfileMapper.selectById(userId);

        if (profile == null) {
            profile = new SysUserProfile();
            profile.setUserId(userId);
            profile.setPreferences(settings);
            profile.setIsPublic(isPublicProfile(settings) ? 1 : 0);
            userProfileMapper.insert(profile);
        } else {
            profile.setPreferences(settings);
            profile.setIsPublic(isPublicProfile(settings) ? 1 : 0);
            userProfileMapper.updateById(profile);
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

    private boolean isPublicProfile(UserSettingsDto settings) {
        return settings != null
                && settings.getPrivacySettings() != null
                && Boolean.TRUE.equals(settings.getPrivacySettings().getPublicProfile());
    }
}
