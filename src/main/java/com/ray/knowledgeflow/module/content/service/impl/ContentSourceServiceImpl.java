package com.ray.knowledgeflow.module.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ray.knowledgeflow.common.exception.BusinessException;
import com.ray.knowledgeflow.common.utils.OutboundUrlValidator;
import com.ray.knowledgeflow.module.content.dto.ContentSourceDto;
import com.ray.knowledgeflow.module.content.entity.ContentSource;
import com.ray.knowledgeflow.module.content.mapper.ContentSourceMapper;
import com.ray.knowledgeflow.module.content.service.IArXivFetchService;
import com.ray.knowledgeflow.module.content.service.IContentSourceService;
import com.ray.knowledgeflow.module.content.service.IGitHubFetchService;
import com.ray.knowledgeflow.module.content.service.IRssFetchService;
import com.ray.knowledgeflow.module.content.service.IContentItemService;
import com.ray.knowledgeflow.module.content.message.ContentFetchMessage;
import com.ray.knowledgeflow.module.content.message.ContentFetchProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 内容源服务实现类
 *
 * @author Ray
 * @since 2024-01-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentSourceServiceImpl extends ServiceImpl<ContentSourceMapper, ContentSource>
        implements IContentSourceService {

    private final IRssFetchService rssFetchService;
    private final IGitHubFetchService gitHubFetchService;
    private final IArXivFetchService arXivFetchService;
    private final ContentFetchProducer contentFetchProducer;
    private final IContentItemService contentItemService;

    @Override
    public List<ContentSourceDto> listByUserId(Long userId) {
        List<ContentSource> sources = this.list(new LambdaQueryWrapper<ContentSource>()
                .eq(ContentSource::getUserId, userId)
                .orderByDesc(ContentSource::getCreatedTime));

        return sources.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContentSourceDto addSource(Long userId, ContentSourceDto dto) {
        validateSourceUrl(dto.getType(), dto.getUrl());

        // 检查是否已存在相同 URL
        long count = this.count(new LambdaQueryWrapper<ContentSource>()
                .eq(ContentSource::getUserId, userId)
                .eq(ContentSource::getUrl, dto.getUrl()));
        if (count > 0) {
            throw new BusinessException(400, "该订阅源已存在");
        }

        ContentSource source = new ContentSource();
        source.setUserId(userId);
        source.setType(dto.getType());
        source.setName(dto.getName());
        source.setUrl(dto.getUrl());
        source.setSyncInterval(dto.getSyncInterval() != null ? dto.getSyncInterval() : 60);
        source.setStatus(1);

        this.save(source);

        // 首次抓取只在内容源事务成功提交后入队。
        submitFetchAfterCommit(source);

        return toDto(source);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContentSourceDto updateSource(Long userId, Long sourceId, ContentSourceDto dto) {
        ContentSource source = getAndCheckOwnership(userId, sourceId);

        if (dto.getName() != null) {
            source.setName(dto.getName());
        }
        if (dto.getSyncInterval() != null) {
            source.setSyncInterval(dto.getSyncInterval());
        }

        this.updateById(source);
        return toDto(source);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSource(Long userId, Long sourceId) {
        ContentSource source = getAndCheckOwnership(userId, sourceId);
        int deletedItems = contentItemService.deleteBySource(userId, sourceId);
        this.removeById(source.getId());
        log.info("删除内容源及关联内容: sourceId={}, items={}", sourceId, deletedItems);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long userId, Long sourceId, Integer status) {
        ContentSource source = getAndCheckOwnership(userId, sourceId);
        source.setStatus(status);
        this.updateById(source);
    }

    @Override
    public void syncNow(Long userId, Long sourceId) {
        ContentSource source = getAndCheckOwnership(userId, sourceId);
        fetchSynchronously(source);
        source.setLastSyncTime(LocalDateTime.now());
        this.updateById(source);
    }

    @Override
    public void syncNowAsync(Long userId, Long sourceId) {
        ContentSource source = getAndCheckOwnership(userId, sourceId);

        // 发送消息到 RabbitMQ
        ContentFetchMessage message = new ContentFetchMessage(
                source.getType(),
                source.getId(),
                userId,
                null);
        contentFetchProducer.sendFetchTask(message);

        log.info("已发送异步同步任务: sourceId={}, type={}", sourceId, source.getType());
    }

    // ==========私有方法 ==========

    private ContentSource getAndCheckOwnership(Long userId, Long sourceId) {
        ContentSource source = this.getById(sourceId);
        if (source == null) {
            throw new BusinessException(404, "内容源不存在");
        }
        if (!source.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作此内容源");
        }
        return source;
    }

    private ContentSourceDto toDto(ContentSource source) {
        ContentSourceDto dto = new ContentSourceDto();
        BeanUtils.copyProperties(source, dto);
        return dto;
    }

    private void fetchSynchronously(ContentSource source) {
        switch (source.getType().toUpperCase()) {
            case "RSS":
                rssFetchService.fetch(source);
                break;
            case "GITHUB":
                gitHubFetchService.fetch(source);
                break;
            case "ARXIV":
                arXivFetchService.fetch(source);
                break;
            default:
                throw new BusinessException(400, "不支持的内容源类型");
        }
    }

    private void submitFetchAfterCommit(ContentSource source) {
        Runnable submit = () -> contentFetchProducer.sendFetchTask(new ContentFetchMessage(
                source.getType(), source.getId(), source.getUserId(), null));
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    submit.run();
                }
            });
        } else {
            submit.run();
        }
    }

    private void validateSourceUrl(String type, String url) {
        if (type == null || type.isBlank()) {
            throw new BusinessException(400, "源类型不能为空");
        }
        try {
            switch (type.toUpperCase()) {
                case "RSS" -> OutboundUrlValidator.requirePublicHttpUrl(url);
                case "GITHUB" -> OutboundUrlValidator.requireHost(url, true, Set.of("api.github.com"));
                case "ARXIV" -> OutboundUrlValidator.requireHost(url, false, Set.of("export.arxiv.org"));
                case "FILE" -> {
                    // FILE 类型由文件上传链路处理，不发起任意网络请求。
                }
                default -> throw new IllegalArgumentException("不支持的内容源类型");
            }
        } catch (IllegalArgumentException e) {
            throw new BusinessException(400, e.getMessage());
        }
    }
}
