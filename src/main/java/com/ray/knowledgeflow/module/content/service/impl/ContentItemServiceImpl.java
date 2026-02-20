package com.ray.knowledgeflow.module.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ray.knowledgeflow.common.exception.BusinessException;
import com.ray.knowledgeflow.module.content.dto.ContentItemDto;
import com.ray.knowledgeflow.module.content.entity.ContentItem;
import com.ray.knowledgeflow.module.content.entity.ContentSource;
import com.ray.knowledgeflow.module.content.mapper.ContentItemMapper;
import com.ray.knowledgeflow.module.content.mapper.ContentSourceMapper;
import com.ray.knowledgeflow.module.content.service.IContentItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;

/**
 * 内容项服务实现类
 *
 * @author Ray
 * @since 2024-01-16
 */
@Service
@RequiredArgsConstructor
public class ContentItemServiceImpl extends ServiceImpl<ContentItemMapper, ContentItem>
        implements IContentItemService {

    private final ContentSourceMapper contentSourceMapper;

    @Override
    public IPage<ContentItemDto> pageByUserId(Long userId, Integer status, Long sourceId,
            String keyword, int page, int size) {
        LambdaQueryWrapper<ContentItem> wrapper = new LambdaQueryWrapper<ContentItem>()
                .eq(ContentItem::getUserId, userId)
                .eq(status != null, ContentItem::getStatus, status)
                .eq(sourceId != null, ContentItem::getSourceId, sourceId)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(ContentItem::getTitle, keyword)
                        .or()
                        .like(ContentItem::getSummary, keyword))
                .orderByDesc(ContentItem::getPublishTime);

        Page<ContentItem> pageResult = this.page(new Page<>(page, size), wrapper);

        return pageResult.convert(this::toDto);
    }

    @Override
    public ContentItemDto getDetail(Long userId, Long itemId) {
        ContentItem item = getAndCheckOwnership(userId, itemId);
        ContentItemDto dto = toDto(item);

        // 标记为已读
        if (item.getStatus() == 0) {
            item.setStatus(1);
            this.updateById(item);
        }

        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long userId, Long itemId, Integer status) {
        ContentItem item = getAndCheckOwnership(userId, itemId);
        item.setStatus(status);
        this.updateById(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateStatus(Long userId, Long[] itemIds, Integer status) {
        for (Long itemId : itemIds) {
            ContentItem item = this.getById(itemId);
            if (item != null && item.getUserId().equals(userId)) {
                item.setStatus(status);
                this.updateById(item);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteItem(Long userId, Long itemId) {
        ContentItem item = getAndCheckOwnership(userId, itemId);
        this.removeById(item.getId());
    }

    // ========== 私有方法 ==========

    private ContentItem getAndCheckOwnership(Long userId, Long itemId) {
        ContentItem item = this.getById(itemId);
        if (item == null) {
            throw new BusinessException(404, "内容不存在");
        }
        if (!item.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作此内容");
        }
        return item;
    }

    private ContentItemDto toDto(ContentItem item) {
        ContentItemDto dto = new ContentItemDto();
        BeanUtils.copyProperties(item, dto);

        // 获取来源名称
        if (item.getSourceId() != null) {
            ContentSource source = contentSourceMapper.selectById(item.getSourceId());
            if (source != null) {
                dto.setSourceName(source.getName());
            }
        }

        return dto;
    }
}
