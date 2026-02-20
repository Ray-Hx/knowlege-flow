package com.ray.knowledgeflow.module.content.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ray.knowledgeflow.module.content.dto.ContentSourceDto;
import com.ray.knowledgeflow.module.content.entity.ContentSource;

import java.util.List;

/**
 * 内容源服务接口
 *
 * @author Ray
 * @since 2024-01-16
 */
public interface IContentSourceService extends IService<ContentSource> {

    /**
     * 获取用户的所有内容源
     */
    List<ContentSourceDto> listByUserId(Long userId);

    /**
     * 添加内容源
     */
    ContentSourceDto addSource(Long userId, ContentSourceDto dto);

    /**
     * 更新内容源
     */
    ContentSourceDto updateSource(Long userId, Long sourceId, ContentSourceDto dto);

    /**
     * 删除内容源
     */
    void deleteSource(Long userId, Long sourceId);

    /**
     * 启用/禁用内容源
     */
    void toggleStatus(Long userId, Long sourceId, Integer status);

    /**
     * 立即同步内容源
     */
    void syncNow(Long userId, Long sourceId);

    /**
     * 立即同步内容源 (异步，发送到 MQ)
     */
    void syncNowAsync(Long userId, Long sourceId);
}
