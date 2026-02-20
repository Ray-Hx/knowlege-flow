package com.ray.knowledgeflow.module.content.service;

import com.ray.knowledgeflow.module.content.entity.ContentSource;

/**
 * arXiv抓取服务接口
 *
 * @author Ray
 * @since 2024-01-16
 */
public interface IArXivFetchService {

    /**
     * 从 arXiv 检索论文
     * 
     * @param sourceId 内容源ID
     * @param userId   用户ID
     */
    void fetchFromSource(Long sourceId, Long userId);

    /**
     * 异步搜索arXiv论文
     *
     * @param source 数据源
     */
    void fetchAsync(ContentSource source);

    /**
     * 同步搜索arXiv论文
     *
     * @param source 数据源
     * @return 新增论文数量
     */
    int fetch(ContentSource source);
}
