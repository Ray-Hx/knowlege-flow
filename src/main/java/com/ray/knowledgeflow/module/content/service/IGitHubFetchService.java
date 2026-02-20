package com.ray.knowledgeflow.module.content.service;

import com.ray.knowledgeflow.module.content.entity.ContentSource;

/**
 * GitHub抓取服务接口
 *
 * @author Ray
 * @since 2024-01-16
 */
public interface IGitHubFetchService {

    /**
     * 异步抓取GitHub starred仓库
     *
     * @param source 数据源
     */
    void fetchAsync(ContentSource source);

    /**
     * 从 GitHub 仓库抓取内容
     * 
     * @param sourceId 内容源ID
     * @param userId   用户ID
     */
    void fetchFromSource(Long sourceId, Long userId);

    /**
     * 同步抓取GitHub starred仓库
     *
     * @param source 数据源
     * @return 新增内容数量
     */
    int fetch(ContentSource source);
}
