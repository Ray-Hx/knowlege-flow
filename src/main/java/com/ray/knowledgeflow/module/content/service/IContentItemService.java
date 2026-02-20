package com.ray.knowledgeflow.module.content.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ray.knowledgeflow.module.content.dto.ContentItemDto;
import com.ray.knowledgeflow.module.content.entity.ContentItem;

/**
 * 内容项服务接口
 *
 * @author Ray
 * @since 2024-01-16
 */
public interface IContentItemService extends IService<ContentItem> {

    /**
     * 分页查询用户的内容项
     *
     * @param userId   用户 ID
     * @param status   状态过滤 (可选)
     * @param sourceId 来源过滤 (可选)
     * @param keyword  关键词搜索 (可选)
     * @param page     页码
     * @param size     每页数量
     */
    IPage<ContentItemDto> pageByUserId(Long userId, Integer status, Long sourceId, String keyword, int page, int size);

    /**
     * 获取内容详情
     */
    ContentItemDto getDetail(Long userId, Long itemId);

    /**
     * 更新内容状态
     */
    void updateStatus(Long userId, Long itemId, Integer status);

    /**
     * 批量更新状态
     */
    void batchUpdateStatus(Long userId, Long[] itemIds, Integer status);

    /**
     * 删除内容项
     */
    void deleteItem(Long userId, Long itemId);
}
