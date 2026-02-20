package com.ray.knowledgeflow.module.note.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ray.knowledgeflow.module.note.entity.NoteLike;
import com.ray.knowledgeflow.module.note.mapper.NoteLikeMapper;
import com.ray.knowledgeflow.module.note.service.NoteEngagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

    // Redis key 前缀
    private static final String VIEW_COUNT_KEY = "note:view:";
    private static final String LIKE_COUNT_KEY = "note:like:count:";
    private static final String USER_LIKE_KEY = "note:like:user:";

    // 缓存过期时间（7天）
    private static final long CACHE_EXPIRE_DAYS = 7;

    @Override
    public void incrementViewCount(Long noteId) {
        String key = VIEW_COUNT_KEY + noteId;
        redisTemplate.opsForValue().increment(key);
        // 设置过期时间（如果是新key）
        if (redisTemplate.getExpire(key) == -1) {
            redisTemplate.expire(key, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
        }
    }

    @Override
    public Long getViewCount(Long noteId) {
        String key = VIEW_COUNT_KEY + noteId;
        Object count = redisTemplate.opsForValue().get(key);
        if (count == null) {
            return 0L;
        }
        return Long.valueOf(count.toString());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LikeResult toggleLike(Long userId, Long noteId) {
        String userLikeKey = USER_LIKE_KEY + noteId + ":" + userId;
        String countKey = LIKE_COUNT_KEY + noteId;

        // 检查是否已点赞
        boolean alreadyLiked = isLikedFromDb(userId, noteId);

        if (alreadyLiked) {
            // 取消点赞
            noteLikeMapper.delete(new LambdaQueryWrapper<NoteLike>()
                    .eq(NoteLike::getNoteId, noteId)
                    .eq(NoteLike::getUserId, userId));
            redisTemplate.delete(userLikeKey);
            redisTemplate.opsForValue().decrement(countKey);

            Long newCount = getLikeCount(noteId);
            return new LikeResult(false, newCount);
        } else {
            // 点赞
            NoteLike like = new NoteLike();
            like.setNoteId(noteId);
            like.setUserId(userId);
            noteLikeMapper.insert(like);

            redisTemplate.opsForValue().set(userLikeKey, "1", CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
            redisTemplate.opsForValue().increment(countKey);

            Long newCount = getLikeCount(noteId);
            return new LikeResult(true, newCount);
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

        for (Long noteId : noteIds) {
            Long viewCount = getViewCount(noteId);
            Long likeCount = getLikeCount(noteId);
            result.put(noteId, new EngagementStats(viewCount, likeCount));
        }

        return result;
    }
}
