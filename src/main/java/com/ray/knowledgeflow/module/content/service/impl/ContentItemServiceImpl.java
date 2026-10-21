package com.ray.knowledgeflow.module.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import com.ray.knowledgeflow.module.search.event.ContentDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public IPage<ContentItemDto> pageByUserId(Long userId, Integer status, Long sourceId, Boolean isUpload,
            String keyword, int page, int size) {
        LambdaQueryWrapper<ContentItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ContentItem::getUserId, userId);
        queryWrapper.eq(ContentItem::getDeleted, 0);

        if (status != null) {
            queryWrapper.eq(ContentItem::getStatus, status);
        }
        if (sourceId != null) {
            queryWrapper.eq(ContentItem::getSourceId, sourceId);
        }
        if (isUpload != null) {
            if (isUpload) {
                queryWrapper.isNull(ContentItem::getSourceId);
            } else {
                queryWrapper.isNotNull(ContentItem::getSourceId);
            }
        }
        queryWrapper.and(StringUtils.hasText(keyword), w -> w
                .like(ContentItem::getTitle, keyword)
                .or()
                .like(ContentItem::getSummary, keyword))
                .orderByDesc(ContentItem::getPublishTime);

        Page<ContentItem> pageResult = this.page(new Page<>(page, size), queryWrapper);

        Set<Long> sourceIds = pageResult.getRecords().stream()
                .map(ContentItem::getSourceId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, ContentSource> sourceMap = sourceIds.isEmpty()
                ? Collections.emptyMap()
                : contentSourceMapper.selectList(new LambdaQueryWrapper<ContentSource>()
                                .in(ContentSource::getId, sourceIds)
                                .eq(ContentSource::getUserId, userId))
                        .stream()
                        .collect(Collectors.toMap(ContentSource::getId, Function.identity()));

        return pageResult.convert(item -> toDto(item, sourceMap));
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
        if (itemIds == null || itemIds.length == 0) {
            return;
        }
        java.util.List<Long> distinctIds = java.util.Arrays.stream(itemIds)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (distinctIds.isEmpty()) {
            return;
        }
        ContentItem update = new ContentItem();
        update.setStatus(status);
        this.update(update, new LambdaUpdateWrapper<ContentItem>()
                .eq(ContentItem::getUserId, userId)
                .in(ContentItem::getId, distinctIds));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteItem(Long userId, Long itemId) {
        ContentItem item = getAndCheckOwnership(userId, itemId);
        this.removeById(item.getId());
        // 发布内容同步更新事件
        eventPublisher.publishEvent(new ContentDeletedEvent(this, itemId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteBySource(Long userId, Long sourceId) {
        java.util.List<ContentItem> items = this.list(new LambdaQueryWrapper<ContentItem>()
                .select(ContentItem::getId)
                .eq(ContentItem::getUserId, userId)
                .eq(ContentItem::getSourceId, sourceId));
        if (items.isEmpty()) {
            return 0;
        }
        java.util.List<Long> ids = items.stream().map(ContentItem::getId).toList();
        this.remove(new LambdaQueryWrapper<ContentItem>()
                .eq(ContentItem::getUserId, userId)
                .eq(ContentItem::getSourceId, sourceId));
        ids.forEach(id -> eventPublisher.publishEvent(new ContentDeletedEvent(this, id)));
        return ids.size();
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
        ContentSource source = item.getSourceId() == null ? null : contentSourceMapper.selectById(item.getSourceId());
        return toDto(item, source == null ? Collections.emptyMap() : Map.of(source.getId(), source));
    }

    private ContentItemDto toDto(ContentItem item, Map<Long, ContentSource> sourceMap) {
        ContentItemDto dto = new ContentItemDto();
        BeanUtils.copyProperties(item, dto);

        ContentSource source = sourceMap.get(item.getSourceId());
        if (source != null) {
            dto.setSourceName(source.getName());
        }

        return dto;
    }
}
