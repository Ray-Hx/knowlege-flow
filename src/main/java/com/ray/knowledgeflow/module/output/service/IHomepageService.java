package com.ray.knowledgeflow.module.output.service;

import com.ray.knowledgeflow.module.output.dto.HomepageConfigDto;
import com.ray.knowledgeflow.module.output.dto.PublicHomepageDto;

/**
 * 主页服务接口
 *
 * @author Ray
 * @since 2024-01-26
 */
public interface IHomepageService {

    /**
     * 获取用户主页配置
     *
     * @param userId 用户ID
     * @return 主页配置
     */
    HomepageConfigDto getConfig(Long userId);

    /**
     * 更新主页配置
     *
     * @param userId 用户ID
     * @param dto    配置数据
     * @return 更新后的配置
     */
    HomepageConfigDto updateConfig(Long userId, HomepageConfigDto dto);

    /**
     * 获取公开主页数据
     *
     * @param slug 个性化URL
     * @return 公开主页数据
     */
    PublicHomepageDto getPublicHomepage(String slug);

    /**
     * 检查slug是否可用
     *
     * @param slug 个性化URL
     * @return 是否可用
     */
    boolean isSlugAvailable(String slug);
}
