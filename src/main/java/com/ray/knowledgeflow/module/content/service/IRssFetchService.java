package com.ray.knowledgeflow.module.content.service;

import com.ray.knowledgeflow.module.content.entity.ContentSource;

/**
 * RSS 抓取服务接口
 *
 * @author Ray
 * @since 2024-01-16
 */
public interface IRssFetchService {

    /**
     * 从 RSS 源抓取内容
     * 
     * @param sourceId 内容源ID
     * @param userId   用户ID
     */
    void fetchFromSource(Long sourceId, Long userId);

    /**
     * 异步抓取 RSS 订阅内容
     *
     * @param source 内容源
     */
    void fetchAsync(ContentSource source);

    /**
     * 同步抓取 RSS 订阅内容
     *
     * @param source 内容源
     * @return 抓取的新条目数量
     */
    int fetch(ContentSource source);
}
