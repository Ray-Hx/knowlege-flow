package com.ray.knowledgeflow.module.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ray.knowledgeflow.common.exception.BusinessException;
import com.ray.knowledgeflow.module.user.dto.ChangePasswordDto;
import com.ray.knowledgeflow.module.user.dto.UpdateProfileDto;
import com.ray.knowledgeflow.module.user.dto.UserProfileDto;
import com.ray.knowledgeflow.module.user.entity.SysUser;
import com.ray.knowledgeflow.module.user.entity.SysUserProfile;
import com.ray.knowledgeflow.module.user.mapper.UserProfileMapper;
import com.ray.knowledgeflow.module.user.service.IUserProfileService;
import com.ray.knowledgeflow.module.user.service.IUserService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

/**
 * <p>
 * 用户资料服务实现类
 * </p>
 *
 * @author Ray
 * @since 2024-01-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl extends ServiceImpl<UserProfileMapper, SysUserProfile>
        implements IUserProfileService {

    private final IUserService userService;
    private final PasswordEncoder passwordEncoder;
    private final MinioClient minioClient;

    @Value("${minio.bucket-name:knowledge-flow}")
    private String bucketName;

    @Value("${minio.endpoint:http://localhost:9000}")
    private String minioEndpoint;

    @Override
    public UserProfileDto getProfile(Long userId) {
        SysUser user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        SysUserProfile profile = this.getById(userId);

        UserProfileDto dto = new UserProfileDto();
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setAvatar(user.getAvatar());

        if (profile != null) {
            dto.setBio(profile.getBio());
            dto.setGithubId(profile.getGithubId());
        }

        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileDto updateProfile(Long userId, UpdateProfileDto dto) {
        // 更新用户基本信息
        SysUser user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
            userService.updateById(user);
        }

        // 更新或创建扩展信息
        SysUserProfile profile = this.getById(userId);
        if (profile == null) {
            profile = new SysUserProfile();
            profile.setUserId(userId);
        }

        if (dto.getBio() != null) {
            profile.setBio(dto.getBio());
        }
        if (dto.getGithubId() != null) {
            profile.setGithubId(dto.getGithubId());
        }

        this.saveOrUpdate(profile);

        return getProfile(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String uploadAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择要上传的头像文件");
        }

        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(400, "仅支持上传图片文件");
        }

        // 生成文件名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String objectName = "avatars/" + userId + "/" + UUID.randomUUID() + extension;

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(contentType)
                            .build());
        } catch (Exception e) {
            log.error("头像上传失败", e);
            throw new BusinessException(500, "头像上传失败，请稍后重试");
        }

        // 构建访问 URL
        String avatarUrl = minioEndpoint + "/" + bucketName + "/" + objectName;

        // 更新用户头像
        SysUser user = userService.getById(userId);
        user.setAvatar(avatarUrl);
        userService.updateById(user);

        return avatarUrl;
    }

    @Override
    public void changePassword(Long userId, ChangePasswordDto dto) {
        SysUser user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(400, "原密码错误");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userService.updateById(user);
    }
}
