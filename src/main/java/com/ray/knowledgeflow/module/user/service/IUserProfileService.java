package com.ray.knowledgeflow.module.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ray.knowledgeflow.module.user.dto.ChangePasswordDto;
import com.ray.knowledgeflow.module.user.dto.UpdateProfileDto;
import com.ray.knowledgeflow.module.user.dto.UserProfileDto;
import com.ray.knowledgeflow.module.user.entity.SysUserProfile;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 * 用户资料服务接口
 * </p>
 *
 * @author Ray
 * @since 2024-01-16
 */
public interface IUserProfileService extends IService<SysUserProfile> {

    /**
     * 获取当前用户资料
     *
     * @param userId 用户ID
     * @return 用户资料
     */
    UserProfileDto getProfile(Long userId);

    /**
     * 更新用户资料
     *
     * @param userId 用户ID
     * @param dto    更新参数
     * @return 更新后的资料
     */
    UserProfileDto updateProfile(Long userId, UpdateProfileDto dto);

    /**
     * 上传头像
     *
     * @param userId 用户ID
     * @param file   头像文件
     * @return 头像 URL
     */
    String uploadAvatar(Long userId, MultipartFile file);

    /**
     * 修改密码
     *
     * @param userId 用户ID
     * @param dto    修改密码参数
     */
    void changePassword(Long userId, ChangePasswordDto dto);
}
