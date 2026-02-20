package com.ray.knowledgeflow.module.search.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ray.knowledgeflow.module.content.entity.ContentItem;
import com.ray.knowledgeflow.module.content.mapper.ContentItemMapper;
import com.ray.knowledgeflow.module.note.entity.NoteEntry;
import com.ray.knowledgeflow.module.note.mapper.NoteEntryMapper;
import com.ray.knowledgeflow.module.search.dto.RecommendationDto;
import com.ray.knowledgeflow.module.search.service.IRecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 推荐服务实现
 * 基于用户最近活动和内容相关性进行智能推荐
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements IRecommendationService {

        private final NoteEntryMapper noteMapper;
        private final ContentItemMapper contentMapper;

        @Override
        public List<RecommendationDto> getRecommendations(Long userId, int limit) {
                List<RecommendationDto> recommendations = new ArrayList<>();

                // 1. 最近编辑的笔记 (用户可能想继续阅读)
                List<NoteEntry> recentNotes = noteMapper.selectList(
                                new LambdaQueryWrapper<NoteEntry>()
                                                .eq(NoteEntry::getUserId, userId)
                                                .eq(NoteEntry::getDeleted, 0)
                                                .orderByDesc(NoteEntry::getUpdatedTime)
                                                .last("LIMIT 3"));

                for (NoteEntry note : recentNotes) {
                        recommendations.add(RecommendationDto.builder()
                                        .id(note.getId())
                                        .title(note.getTitle())
                                        .summary(note.getSummary())
                                        .type("NOTE")
                                        .reason("最近编辑")
                                        .updatedTime(note.getUpdatedTime())
                                        .build());
                }

                // 2. 最新订阅内容 (用户可能感兴趣的新内容)
                List<ContentItem> recentContent = contentMapper.selectList(
                                new LambdaQueryWrapper<ContentItem>()
                                                .eq(ContentItem::getUserId, userId)
                                                .eq(ContentItem::getStatus, ContentItem.STATUS_UNREAD) // 未读
                                                .orderByDesc(ContentItem::getPublishTime)
                                                .last("LIMIT 3"));

                for (ContentItem content : recentContent) {
                        recommendations.add(RecommendationDto.builder()
                                        .id(content.getId())
                                        .title(content.getTitle())
                                        .summary(content.getSummary())
                                        .type("CONTENT")
                                        .reason("未读订阅")
                                        .url(content.getUrl())
                                        .updatedTime(content.getPublishTime())
                                        .build());
                }

                // 3. 热门笔记 (最近修改频繁的笔记)
                if (recommendations.size() < limit) {
                        List<NoteEntry> frequentNotes = noteMapper.selectList(
                                        new LambdaQueryWrapper<NoteEntry>()
                                                        .eq(NoteEntry::getUserId, userId)
                                                        .eq(NoteEntry::getDeleted, 0)
                                                        .orderByDesc(NoteEntry::getUpdatedTime) // 使用更新时间代替版本
                                                        .last("LIMIT 4"));
                        for (NoteEntry note : frequentNotes) {
                                // 避免重复
                                boolean exists = recommendations.stream()
                                                .anyMatch(r -> r.getType().equals("NOTE")
                                                                && r.getId().equals(note.getId()));
                                if (!exists) {
                                        recommendations.add(RecommendationDto.builder()
                                                        .id(note.getId())
                                                        .title(note.getTitle())
                                                        .summary(note.getSummary())
                                                        .type("NOTE")
                                                        .reason("常用笔记")
                                                        .updatedTime(note.getUpdatedTime())
                                                        .build());
                                }
                        }
                }

                // 限制返回数量
                return recommendations.stream()
                                .limit(limit)
                                .collect(Collectors.toList());
        }
}
