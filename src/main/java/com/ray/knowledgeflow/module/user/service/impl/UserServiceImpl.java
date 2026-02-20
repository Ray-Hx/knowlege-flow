package com.ray.knowledgeflow.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ray.knowledgeflow.common.exception.BusinessException;
import com.ray.knowledgeflow.common.utils.MinioUtil;
import com.ray.knowledgeflow.module.user.dto.ChangePasswordDto;
import com.ray.knowledgeflow.module.user.dto.LoginDto;
import com.ray.knowledgeflow.module.user.dto.RegisterDto;
import com.ray.knowledgeflow.module.user.dto.UpdateProfileDto;
import com.ray.knowledgeflow.module.user.dto.UserInfoDto;
import com.ray.knowledgeflow.module.user.entity.SysUser;
import com.ray.knowledgeflow.module.user.entity.SysUserProfile;
import com.ray.knowledgeflow.module.user.entity.SysRole;
import com.ray.knowledgeflow.module.user.mapper.UserMapper;
import com.ray.knowledgeflow.module.user.mapper.UserProfileMapper;
import com.ray.knowledgeflow.module.user.service.CaptchaService;
import com.ray.knowledgeflow.module.user.service.IUserService;
import com.ray.knowledgeflow.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * 系统用户服务实现类
 * </p>
 *
 * @author Ray
 * @since 2024-01-14
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, SysUser> implements IUserService {

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserProfileMapper userProfileMapper;
    private final MinioUtil minioUtil;
    private final CaptchaService captchaService;

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp");
    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024; // 5MB

    @Override
    public SysUser getByUsername(String username) {
        return this.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser register(RegisterDto registerDto) {
        // 验证验证码
        if (!captchaService.verify(registerDto.getCaptchaKey(), registerDto.getCaptchaCode())) {
            throw new BusinessException(400, "验证码错误或已过期");
        }

        // 检查用户名是否存在
        if (getByUsername(registerDto.getUsername()) != null) {
            throw new BusinessException(400, "用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(registerDto.getUsername());
        user.setPassword(passwordEncoder.encode(registerDto.getPassword()));
        user.setEmail(registerDto.getEmail());
        user.setStatus(1); // 默认启用

        this.save(user);

        // 创建用户资料记录
        SysUserProfile profile = new SysUserProfile();
        profile.setUserId(user.getId());
        profile.setPreferences(
                "{\"interests\":[],\"preferredCategories\":[],\"uiSettings\":{\"theme\":\"light\",\"language\":\"zh-CN\"}}");
        userProfileMapper.insert(profile);

        return user;
    }

    @Override
    public String login(LoginDto loginDto) {
        // 验证验证码
        if (!captchaService.verify(loginDto.getCaptchaKey(), loginDto.getCaptchaCode())) {
            throw new BusinessException(400, "验证码错误或已过期");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDto.getUsername(), loginDto.getPassword()));

        if (authentication.isAuthenticated()) {
            return jwtUtils.generateToken(loginDto.getUsername());
        } else {
            throw new BusinessException(401, "用户名或密码错误");
        }
    }

    @Override
    public UserInfoDto getCurrentUserInfo() {
        SysUser user = getCurrentUser();
        SysUserProfile profile = userProfileMapper.selectById(user.getId());

        return buildUserInfoDto(user, profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserInfoDto updateProfile(UpdateProfileDto updateProfileDto) {
        SysUser user = getCurrentUser();
        SysUserProfile profile = userProfileMapper.selectById(user.getId());

        if (profile == null) {
            // 如果不存在，创建新的
            profile = new SysUserProfile();
            profile.setUserId(user.getId());
            userProfileMapper.insert(profile);
        }

        // 更新资料
        if (updateProfileDto.getBio() != null) {
            profile.setBio(updateProfileDto.getBio());
        }
        if (updateProfileDto.getPreferences() != null) {
            profile.setPreferences(updateProfileDto.getPreferences());
        }
        if (updateProfileDto.getLearningGoals() != null) {
            profile.setLearningGoals(updateProfileDto.getLearningGoals());
        }

        userProfileMapper.updateById(profile);

        return buildUserInfoDto(user, profile);
    }

    @Override
    public void changePassword(ChangePasswordDto changePasswordDto) {
        SysUser user = getCurrentUser();

        // 验证旧密码
        if (!passwordEncoder.matches(changePasswordDto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(400, "旧密码错误");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(changePasswordDto.getNewPassword()));
        this.updateById(user);
    }

    @Override
    public String uploadAvatar(MultipartFile file) {
        // 验证文件类型
        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new BusinessException(400, "只支持 JPEG、PNG、GIF、WebP 格式的图片");
        }

        // 验证文件大小
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new BusinessException(400, "头像文件不能超过 5MB");
        }

        // 上传到 MinIO
        String avatarUrl = minioUtil.uploadFile(file, "avatars");

        // 更新用户头像
        SysUser user = getCurrentUser();
        user.setAvatar(avatarUrl);
        this.updateById(user);

        return avatarUrl;
    }

    @Override
    public List<SysRole> getUserRoles(Long userId) {
        return baseMapper.selectRolesByUserId(userId);
    }

    // ========== 私有方法 ==========

    /**
     * 获取当前登录用户
     */
    private SysUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(401, "用户未登录");
        }

        String username = authentication.getName();
        SysUser user = getByUsername(username);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        return user;
    }

    /**
     * 构建用户信息 DTO
     */
    private UserInfoDto buildUserInfoDto(SysUser user, SysUserProfile profile) {
        UserInfoDto dto = new UserInfoDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setAvatar(user.getAvatar());
        dto.setStatus(user.getStatus());
        dto.setCreatedTime(user.getCreatedTime());

        if (profile != null) {
            dto.setBio(profile.getBio());
            dto.setGithubId(profile.getGithubId());
            dto.setPreferences(profile.getPreferences());
            dto.setLearningGoals(profile.getLearningGoals());
        }

        // Set roles
        List<SysRole> roles = getUserRoles(user.getId());
        if (roles != null) {
            dto.setRoles(roles.stream().map(SysRole::getRoleName).collect(java.util.stream.Collectors.toList()));
        }

        return dto;
    }
}
