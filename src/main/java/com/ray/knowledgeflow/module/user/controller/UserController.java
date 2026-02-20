package com.ray.knowledgeflow.module.user.controller;

import com.ray.knowledgeflow.common.result.Result;
import com.ray.knowledgeflow.module.user.dto.ChangePasswordDto;
import com.ray.knowledgeflow.module.user.dto.UpdateProfileDto;
import com.ray.knowledgeflow.module.user.dto.UserInfoDto;
import com.ray.knowledgeflow.module.user.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户资料管理控制器
 *
 * @author Ray
 * @since 2024-01-16
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/me")
    public Result<UserInfoDto> getCurrentUser() {
        UserInfoDto userInfo = userService.getCurrentUserInfo();
        return Result.success(userInfo);
    }

    /**
     * 更新用户资料
     * (Deprecated: Moved to UserProfileController)
     */
    // @PutMapping("/profile")
    // public Result<UserInfoDto> updateProfile(@RequestBody @Validated
    // UpdateProfileDto updateProfileDto) {
    // UserInfoDto userInfo = userService.updateProfile(updateProfileDto);
    // return Result.success(userInfo);
    // }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody @Validated ChangePasswordDto changePasswordDto) {
        userService.changePassword(changePasswordDto);
        return Result.success();
    }

    /**
     * 上传头像
     */
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String avatarUrl = userService.uploadAvatar(file);
        return Result.success("头像上传成功", avatarUrl);
    }
}
