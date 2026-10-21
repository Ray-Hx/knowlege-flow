package com.ray.knowledgeflow.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ray.knowledgeflow.module.user.entity.SysRole;
import com.ray.knowledgeflow.module.user.entity.SysUser;
import com.ray.knowledgeflow.module.user.entity.SysUserProfile;
import com.ray.knowledgeflow.module.user.mapper.RoleMapper;
import com.ray.knowledgeflow.module.user.mapper.UserMapper;
import com.ray.knowledgeflow.module.user.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * 初始化 Admin 用户数据
 */
@Slf4j
// @Configuration  // 暂时禁用：sys_role 表不存在，无需角色初始化
@RequiredArgsConstructor
public class AdminDataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(String... args) throws Exception {
        log.info("开始检查初始数据...");

        // 1. 初始化角色
        initRole("ROLE_ADMIN", "管理员", "系统管理员，拥有所有权限");
        initRole("ROLE_USER", "普通用户", "普通注册用户");

        // 2. 初始化 Admin 用户
        initAdminUser();

        log.info("初始数据检查完成");
    }

    private void initRole(String code, String name, String description) {
        if (roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, code)) == null) {
            SysRole role = new SysRole();
            role.setRoleCode(code);
            role.setRoleName(name);
            role.setDescription(description);
            roleMapper.insert(role);
            log.info("初始化角色: {} - {}", code, name);
        }
    }

    private void initAdminUser() {
        String username = "admin";
        if (userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)) == null) {
            SysUser user = new SysUser();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode("admin123"));
            user.setEmail("admin@example.com");
            user.setStatus(1);
            user.setAvatar("https://api.dicebear.com/7.x/avataaars/svg?seed=admin");
            userMapper.insert(user);

            // 初始化 Profile
            SysUserProfile profile = new SysUserProfile();
            profile.setUserId(user.getId());
            profile.setBio("系统管理员");
            com.ray.knowledgeflow.module.user.dto.UserSettingsDto settings = new com.ray.knowledgeflow.module.user.dto.UserSettingsDto();
            settings.setUiSettings(new com.ray.knowledgeflow.module.user.dto.UserSettingsDto.UiSettings());
            settings.getUiSettings().setTheme("light");
            settings.getUiSettings().setLanguage("zh-CN");
            profile.setPreferences(settings);
            userProfileMapper.insert(profile);

            log.info("初始化管理员用户: admin / admin123");
        } else {
            // Optional: Reset admin password if needed, but safe to skip to avoid
            // overwriting changes
            log.info("管理员用户已存在，跳过初始化");
        }
    }
}
