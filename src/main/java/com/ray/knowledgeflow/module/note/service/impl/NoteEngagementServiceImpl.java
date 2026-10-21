package com.ray.knowledgeflow.module.note.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ray.knowledgeflow.module.note.entity.NoteEntry;
import com.ray.knowledgeflow.module.note.entity.NoteLike;
import com.ray.knowledgeflow.module.note.event.NoteLikeChangedEvent;
import com.ray.knowledgeflow.module.note.mapper.NoteEntryMapper;
import com.ray.knowledgeflow.module.note.mapper.NoteLikeMapper;
import com.ray.knowledgeflow.module.note.service.NoteEngagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 笔记互动服务实现
 *
 * @author Ray
 * @since 2026-02-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoteEngagementServiceImpl implements NoteEngagementService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final NoteLikeMapper noteLikeMapper;
    private final NoteEntryMapper noteEntryMapper;
    private final ApplicationEventPublisher eventPublisher;

    // Redis key 前缀
    private static final String VIEW_COUNT_KEY = "note:view:";
    private static final String LIKE_COUNT_KEY = "note:like:count:";
    private static final String USER_LIKE_KEY = "note:like:user:";
    /** 浏览量待回写的笔记 ID 集合 */
    private static final String VIEW_DIRTY_KEY = "note:view:dirty";

    // 缓存过期时间（7天）
    private static final long CACHE_EXPIRE_DAYS = 7;

    @Override
    public void incrementViewCount(Long noteId) {
        // 先确保缓存中是数据库里的累计值，否则自增会从 0 起算，丢掉历史浏览量
        getViewCount(noteId);

        String key = VIEW_COUNT_KEY + noteId;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
        // 标记为待回写，供定时任务落库
        redisTemplate.opsForSet().add(VIEW_DIRTY_KEY, noteId.toString());
    }

    @Override
    public Long getViewCount(Long noteId) {
        String key = VIEW_COUNT_KEY + noteId;
        Object count = redisTemplate.opsForValue().get(key);
        if (count != null) {
            return Long.valueOf(count.toString());
        }

        // 回源数据库，避免缓存过期后浏览量归零
        NoteEntry note = noteEntryMapper.selectById(noteId);
        long dbCount = (note == null || note.getViewCount() == null) ? 0L : note.getViewCount();
        redisTemplate.opsForValue().set(key, dbCount, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
        return dbCount;
    }

    /**
     * 将 Redis 中的浏览量回写数据库。
     *
     * <p>浏览量是高频写、低价值的数据，走 Redis 缓冲后批量落库，避免每次浏览都打数据库。
     */
    @Scheduled(
            fixedDelayString = "${app.engagement.view-flush-delay-ms:600000}",
            initialDelayString = "${app.engagement.view-flush-initial-delay-ms:60000}")
    public void flushViewCounts() {
        Set<Object> dirtyIds = redisTemplate.opsForSet().members(VIEW_DIRTY_KEY);
        if (dirtyIds == null || dirtyIds.isEmpty()) {
            return;
        }

        int flushed = 0;
        for (Object idObj : dirtyIds) {
            try {
                Long noteId = Long.valueOf(idObj.toString());
                Object count = redisTemplate.opsForValue().get(VIEW_COUNT_KEY + noteId);
                if (count == null) {
                    // 缓存已过期，值已在上一轮落库，直接清理标记
                    redisTemplate.opsForSet().remove(VIEW_DIRTY_KEY, idObj);
                    continue;
                }

                NoteEntry update = new NoteEntry();
                update.setId(noteId);
                update.setViewCount(Long.valueOf(count.toString()));
                noteEntryMapper.updateById(update);

                redisTemplate.opsForSet().remove(VIEW_DIRTY_KEY, idObj);
                flushed++;
            } catch (Exception e) {
                log.warn("浏览量回写失败: noteId={}", idObj, e);
            }
        }

        if (flushed > 0) {
            log.debug("浏览量回写完成: {} 条", flushed);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LikeResult toggleLike(Long userId, Long noteId) {
        // 事务内只写数据库。缓存刷新交给事务提交后的消息，
        // 保证数据库回滚时不会留下已被改写的缓存。
        boolean alreadyLiked = isLikedFromDb(userId, noteId);
        boolean liked;

        if (alreadyLiked) {
            noteLikeMapper.delete(new LambdaQueryWrapper<NoteLike>()
                    .eq(NoteLike::getNoteId, noteId)
                    .eq(NoteLike::getUserId, userId));
            liked = false;
        } else {
            NoteLike like = new NoteLike();
            like.setNoteId(noteId);
            like.setUserId(userId);
            noteLikeMapper.insert(like);
            liked = true;
        }

        // 以数据库为准返回，不依赖缓存是否已刷新
        Long newCount = noteLikeMapper.countByNoteId(noteId);

        eventPublisher.publishEvent(
                new NoteLikeChangedEvent(this, noteId, userId, liked ? "LIKE" : "UNLIKE"));

        return new LikeResult(liked, newCount);
    }

    @Override
    public void refreshLikeCountFromDb(Long noteId, Long userId) {
        Long dbCount = noteLikeMapper.countByNoteId(noteId);
        redisTemplate.opsForValue().set(LIKE_COUNT_KEY + noteId, dbCount, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);

        // 失效该用户的点赞状态缓存，下次读取时回源数据库
        if (userId != null) {
            redisTemplate.delete(USER_LIKE_KEY + noteId + ":" + userId);
        }
    }

    @Override
    public Long getLikeCount(Long noteId) {
        String key = LIKE_COUNT_KEY + noteId;
        Object count = redisTemplate.opsForValue().get(key);

        if (count == null) {
            // 从数据库加载并缓存
            Long dbCount = noteLikeMapper.countByNoteId(noteId);
            redisTemplate.opsForValue().set(key, dbCount, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
            return dbCount;
        }

        return Long.valueOf(count.toString());
    }

    @Override
    public boolean isLiked(Long userId, Long noteId) {
        if (userId == null) {
            return false;
        }

        String userLikeKey = USER_LIKE_KEY + noteId + ":" + userId;
        Boolean cached = redisTemplate.hasKey(userLikeKey);

        if (Boolean.TRUE.equals(cached)) {
            return true;
        }

        // 从数据库检查
        boolean liked = isLikedFromDb(userId, noteId);
        if (liked) {
            redisTemplate.opsForValue().set(userLikeKey, "1", CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
        }
        return liked;
    }

    private boolean isLikedFromDb(Long userId, Long noteId) {
        return noteLikeMapper.selectCount(new LambdaQueryWrapper<NoteLike>()
                .eq(NoteLike::getNoteId, noteId)
                .eq(NoteLike::getUserId, userId)) > 0;
    }

    @Override
    public Map<Long, EngagementStats> batchGetEngagement(List<Long> noteIds) {
        Map<Long, EngagementStats> result = new HashMap<>();
        if (noteIds == null || noteIds.isEmpty()) {
            return result;
        }

        // 一次 MGET 取回所有计数，未命中的再批量回源，避免逐条往返
        List<String> viewKeys = noteIds.stream().map(id -> VIEW_COUNT_KEY + id).collect(Collectors.toList());
        List<String> likeKeys = noteIds.stream().map(id -> LIKE_COUNT_KEY + id).collect(Collectors.toList());
        // 返回列表长度与入参不一致时按全部未命中处理，避免下标越界
        List<Object> cachedViews = normalizeCached(redisTemplate.opsForValue().multiGet(viewKeys), noteIds.size());
        List<Object> cachedLikes = normalizeCached(redisTemplate.opsForValue().multiGet(likeKeys), noteIds.size());

        List<Long> missingViews = new ArrayList<>();
        List<Long> missingLikes = new ArrayList<>();
        for (int i = 0; i < noteIds.size(); i++) {
            if (cachedViews.get(i) == null) {
                missingViews.add(noteIds.get(i));
            }
            if (cachedLikes.get(i) == null) {
                missingLikes.add(noteIds.get(i));
            }
        }

        Map<Long, Long> viewFallback = batchLoadViewCounts(missingViews);
        Map<Long, Long> likeFallback = batchLoadLikeCounts(missingLikes);

        for (int i = 0; i < noteIds.size(); i++) {
            Long noteId = noteIds.get(i);
            Object cachedView = cachedViews.get(i);
            Object cachedLike = cachedLikes.get(i);

            long viewCount = cachedView != null ? Long.parseLong(cachedView.toString())
                    : viewFallback.getOrDefault(noteId, 0L);
            long likeCount = cachedLike != null ? Long.parseLong(cachedLike.toString())
                    : likeFallback.getOrDefault(noteId, 0L);

            result.put(noteId, new EngagementStats(viewCount, likeCount));
        }

        return result;
    }

    /**
     * 将 multiGet 结果规整为定长列表，长度不符时视为全部未命中
     */
    private List<Object> normalizeCached(List<Object> cached, int expectedSize) {
        if (cached == null || cached.size() != expectedSize) {
            return new ArrayList<>(Collections.nCopies(expectedSize, null));
        }
        return cached;
    }

    /**
     * 批量回源浏览量并写回缓存
     */
    private Map<Long, Long> batchLoadViewCounts(List<Long> noteIds) {
        if (noteIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Long> counts = new HashMap<>();
        for (NoteEntry note : noteEntryMapper.selectBatchIds(noteIds)) {
            counts.put(note.getId(), note.getViewCount() == null ? 0L : note.getViewCount());
        }
        noteIds.forEach(id -> {
            long value = counts.getOrDefault(id, 0L);
            counts.put(id, value);
            redisTemplate.opsForValue().set(VIEW_COUNT_KEY + id, value, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
        });
        return counts;
    }

    /**
     * 批量回源点赞数并写回缓存
     */
    private Map<Long, Long> batchLoadLikeCounts(List<Long> noteIds) {
        if (noteIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Long> counts = noteLikeMapper.countByNoteIds(noteIds);
        noteIds.forEach(id -> {
            long value = counts.getOrDefault(id, 0L);
            counts.put(id, value);
            redisTemplate.opsForValue().set(LIKE_COUNT_KEY + id, value, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
        });
        return counts;
    }
}
