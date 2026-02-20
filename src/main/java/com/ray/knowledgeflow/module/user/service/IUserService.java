package com.ray.knowledgeflow.module.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ray.knowledgeflow.module.user.dto.*;
import com.ray.knowledgeflow.module.user.entity.SysUser;
import com.ray.knowledgeflow.module.user.entity.SysRole;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 * 系统用户服务类
 * </p>
 *
 * @author Ray
 * @since 2024-01-14
 */
public interface IUserService extends IService<SysUser> {

    /**
     * 根据用户名获取用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    SysUser getByUsername(String username);

    /**
     * 用户注册
     *
     * @param registerDto 注册参数
     * @return 注册成功的用户
     */
    SysUser register(RegisterDto registerDto);

    /**
     * 用户登录
     *
     * @param loginDto 登录参数
     * @return JWT Token
     */
    String login(LoginDto loginDto);

    /**
     * 获取当前登录用户信息
     *
     * @return 用户信息 DTO
     */
    UserInfoDto getCurrentUserInfo();

    /**
     * 更新用户资料
     *
     * @param updateProfileDto 更新参数
     * @return 更新后的用户信息
     */
    UserInfoDto updateProfile(UpdateProfileDto updateProfileDto);

    /**
     * 修改密码
     *
     * @param changePasswordDto 修改密码参数
     */
    void changePassword(ChangePasswordDto changePasswordDto);

    /**
     * 上传头像
     *
     * @param file 头像文件
     * @return 头像 URL
     */
    String uploadAvatar(MultipartFile file);

    /**
     * 获取用户角色列表
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    List<SysRole> getUserRoles(Long userId);
}
