package com.ray.knowledgeflow.module.output.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ray.knowledgeflow.common.exception.BusinessException;
import com.ray.knowledgeflow.module.output.dto.HomepageConfigDto;
import com.ray.knowledgeflow.module.output.dto.PublicHomepageDto;
import com.ray.knowledgeflow.module.output.entity.UserHomepage;
import com.ray.knowledgeflow.module.output.mapper.UserHomepageMapper;
import com.ray.knowledgeflow.module.output.service.IHomepageService;
import com.ray.knowledgeflow.module.user.entity.SysUser;
import com.ray.knowledgeflow.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 主页服务实现
 *
 * @author Ray
 * @since 2024-01-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HomepageServiceImpl implements IHomepageService {

    private final UserHomepageMapper homepageMapper;
    private final UserMapper userMapper;

    @Override
    public HomepageConfigDto getConfig(Long userId) {
        UserHomepage homepage = homepageMapper.selectOne(
                new LambdaQueryWrapper<UserHomepage>()
                        .eq(UserHomepage::getUserId, userId));

        if (homepage == null) {
            // 创建默认配置
            homepage = createDefaultHomepage(userId);
        }

        HomepageConfigDto dto = new HomepageConfigDto();
        BeanUtils.copyProperties(homepage, dto);
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HomepageConfigDto updateConfig(Long userId, HomepageConfigDto dto) {
        UserHomepage homepage = homepageMapper.selectOne(
                new LambdaQueryWrapper<UserHomepage>()
                        .eq(UserHomepage::getUserId, userId));

        if (homepage == null) {
            homepage = new UserHomepage();
            homepage.setUserId(userId);
        }

        // 检查slug唯一性
        if (dto.getSlug() != null && !dto.getSlug().equals(homepage.getSlug())) {
            if (!isSlugAvailable(dto.getSlug())) {
                throw new BusinessException("该个性化URL已被使用");
            }
        }

        BeanUtils.copyProperties(dto, homepage, "id", "userId", "createdTime");

        if (homepage.getId() == null) {
            homepageMapper.insert(homepage);
        } else {
            homepageMapper.updateById(homepage);
        }

        HomepageConfigDto result = new HomepageConfigDto();
        BeanUtils.copyProperties(homepage, result);
        return result;
    }

    @Override
    public PublicHomepageDto getPublicHomepage(String slug) {
        UserHomepage homepage = homepageMapper.selectOne(
                new LambdaQueryWrapper<UserHomepage>()
                        .eq(UserHomepage::getSlug, slug));

        if (homepage == null) {
            throw new BusinessException("主页不存在");
        }

        SysUser user = userMapper.selectById(homepage.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        PublicHomepageDto dto = new PublicHomepageDto();
        dto.setUsername(user.getUsername());
        dto.setAvatar(user.getAvatar());
        dto.setTitle(homepage.getTitle());
        dto.setBio(homepage.getBio());
        dto.setTheme(homepage.getTheme());
        dto.setLayout(homepage.getLayout());
        dto.setShowStats(homepage.getShowStats() == 1);
        dto.setShowSkills(homepage.getShowSkills() == 1);
        dto.setShowNotes(homepage.getShowNotes() == 1);
        dto.setCustomCss(homepage.getCustomCss());

        // TODO: 加载统计数据、技能、笔记数量等
        // 这些可以通过调用其他服务获取

        return dto;
    }

    @Override
    public boolean isSlugAvailable(String slug) {
        Long count = homepageMapper.selectCount(
                new LambdaQueryWrapper<UserHomepage>()
                        .eq(UserHomepage::getSlug, slug));
        return count == 0;
    }

    private UserHomepage createDefaultHomepage(Long userId) {
        SysUser user = userMapper.selectById(userId);

        UserHomepage homepage = new UserHomepage();
        homepage.setUserId(userId);
        homepage.setSlug(user.getUsername());
        homepage.setTitle(user.getUsername() + " 的主页");
        homepage.setTheme("default");
        homepage.setLayout("standard");
        homepage.setShowStats(1);
        homepage.setShowSkills(1);
        homepage.setShowNotes(1);

        homepageMapper.insert(homepage);
        return homepage;
    }
}
