package com.ray.knowledgeflow.module.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ray.knowledgeflow.module.content.entity.ContentItem;
import com.ray.knowledgeflow.module.content.entity.ContentSource;
import com.ray.knowledgeflow.module.content.mapper.ContentItemMapper;
import com.ray.knowledgeflow.module.content.mapper.ContentSourceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ContentItemServicePerformanceTest {

    @Mock private ContentItemMapper contentItemMapper;
    @Mock private ContentSourceMapper contentSourceMapper;
    @Mock private ApplicationEventPublisher eventPublisher;

    private ContentItemServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "test"), ContentItem.class);
        service = new ContentItemServiceImpl(contentSourceMapper, eventPublisher);
        ReflectionTestUtils.setField(service, "baseMapper", contentItemMapper);
    }

    @Test
    void pageByUserId_ShouldLoadSourceNamesInOneBatch() {
        ContentItem first = item(1L, 10L);
        ContentItem second = item(2L, 10L);
        when(contentItemMapper.selectPage(any(Page.class), any(Wrapper.class))).thenAnswer(invocation -> {
            Page<ContentItem> page = invocation.getArgument(0);
            page.setRecords(List.of(first, second));
            page.setTotal(2);
            return page;
        });

        ContentSource source = new ContentSource();
        source.setId(10L);
        source.setUserId(7L);
        source.setName("RSS feed");
        when(contentSourceMapper.selectList(any())).thenReturn(List.of(source));

        var result = service.pageByUserId(7L, null, null, null, null, 1, 10);

        assertEquals(List.of("RSS feed", "RSS feed"), result.getRecords().stream()
                .map(dto -> dto.getSourceName())
                .toList());
        verify(contentSourceMapper).selectList(any());
        verify(contentSourceMapper, never()).selectById(any());
    }

    @Test
    void batchUpdateStatus_ShouldUseOneScopedUpdate() {
        service.batchUpdateStatus(7L, new Long[]{1L, 2L, 2L}, 1);

        verify(contentItemMapper).update(any(ContentItem.class), any(Wrapper.class));
        verify(contentItemMapper, never()).selectById(any());
        verify(contentItemMapper, never()).updateById(any(ContentItem.class));
    }

    @Test
    void deleteBySource_ShouldDeleteInOneStatementAndPublishEveryIndexCleanup() {
        when(contentItemMapper.selectList(any())).thenReturn(List.of(item(1L, 10L), item(2L, 10L)));

        int deleted = service.deleteBySource(7L, 10L);

        assertEquals(2, deleted);
        verify(contentItemMapper).delete(any(Wrapper.class));
        verify(eventPublisher, times(2)).publishEvent(any());
    }

    private static ContentItem item(Long id, Long sourceId) {
        ContentItem item = new ContentItem();
        item.setId(id);
        item.setUserId(7L);
        item.setSourceId(sourceId);
        return item;
    }
}
