package com.ray.knowledgeflow.module.user.controller;

import com.ray.knowledgeflow.common.result.Result;
import com.ray.knowledgeflow.module.user.dto.ChangePasswordDto;
import com.ray.knowledgeflow.module.user.dto.UpdateProfileDto;
import com.ray.knowledgeflow.module.user.dto.UserProfileDto;
import com.ray.knowledgeflow.module.user.service.IUserProfileService;
import com.ray.knowledgeflow.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户资料控制器
 *
 * @author Ray
 * @since 2024-01-16
 */
@RestController
@RequestMapping("/user/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final IUserProfileService userProfileService;

    /**
     * 获取当前用户资料
     */
    @GetMapping
    public Result<UserProfileDto> getProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(userProfileService.getProfile(userId));
    }

    /**
     * 更新用户资料
     */
    @PutMapping
    public Result<UserProfileDto> updateProfile(@RequestBody UpdateProfileDto dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(userProfileService.updateProfile(userId, dto));
    }

    /**
     * 上传头像
     */
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = SecurityUtils.getCurrentUserId();
        String avatarUrl = userProfileService.uploadAvatar(userId, file);
        return Result.success(avatarUrl);
    }

    /**
     * 修改密码
     */
    @PostMapping("/password")
    public Result<Void> changePassword(@RequestBody ChangePasswordDto dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        userProfileService.changePassword(userId, dto);
        return Result.success();
    }
}
