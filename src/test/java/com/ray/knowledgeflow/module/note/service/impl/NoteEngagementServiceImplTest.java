package com.ray.knowledgeflow.module.note.service.impl;

import com.ray.knowledgeflow.module.note.entity.NoteEntry;
import com.ray.knowledgeflow.module.note.event.NoteLikeChangedEvent;
import com.ray.knowledgeflow.module.note.mapper.NoteEntryMapper;
import com.ray.knowledgeflow.module.note.mapper.NoteLikeMapper;
import com.ray.knowledgeflow.module.note.service.NoteEngagementService.LikeResult;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 互动计数的缓存一致性测试。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NoteEngagementServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOps;
    @Mock
    private SetOperations<String, Object> setOps;
    @Mock
    private NoteLikeMapper noteLikeMapper;
    @Mock
    private NoteEntryMapper noteEntryMapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private NoteEngagementServiceImpl service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.opsForSet()).thenReturn(setOps);
    }

    /**
     * 事务内不得改写缓存，只写数据库并发出事件。
     * 否则事务回滚时缓存已被改动，形成双写不一致。
     */
    @Test
    void toggleLike_ShouldNotTouchCacheInsideTransaction() {
        Long noteId = 1L;
        Long userId = 2L;

        when(noteLikeMapper.selectCount(any())).thenReturn(0L);
        when(noteLikeMapper.countByNoteId(noteId)).thenReturn(6L);

        LikeResult result = service.toggleLike(userId, noteId);

        // 返回值以数据库为准，不依赖缓存
        assertEquals(6L, result.getLikeCount());
        assertTrue(result.isLiked());

        // 事务内不碰缓存
        verify(valueOps, never()).increment(anyString());
        verify(valueOps, never()).decrement(anyString());
        verify(valueOps, never()).set(eq("note:like:count:" + noteId), any(), anyLong(), any(TimeUnit.class));

        // 缓存刷新通过事务提交后的事件驱动
        ArgumentCaptor<NoteLikeChangedEvent> captor = ArgumentCaptor.forClass(NoteLikeChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(noteId, captor.getValue().getNoteId());
        assertEquals(userId, captor.getValue().getUserId());
        assertEquals("LIKE", captor.getValue().getAction());
    }

    /**
     * 消费端以数据库为准全量重算，因此重复消费结果一致（幂等）。
     */
    @Test
    void refreshLikeCountFromDb_ShouldBeIdempotent() {
        Long noteId = 1L;
        Long userId = 2L;
        when(noteLikeMapper.countByNoteId(noteId)).thenReturn(3L);

        service.refreshLikeCountFromDb(noteId, userId);
        service.refreshLikeCountFromDb(noteId, userId);

        // 两次消费写入同一个值，不会累加
        verify(valueOps, times(2)).set(eq("note:like:count:" + noteId), eq(3L), anyLong(), any(TimeUnit.class));
        // 同时失效该用户的点赞状态缓存，避免取消点赞后 isLiked 仍返回 true
        verify(redisTemplate, times(2)).delete("note:like:user:" + noteId + ":" + userId);
    }

    /**
     * 缓存过期后浏览量必须回源数据库，而不是归零。
     */
    @Test
    void getViewCount_ShouldFallBackToDatabase() {
        Long noteId = 1L;
        when(valueOps.get("note:view:" + noteId)).thenReturn(null);

        NoteEntry note = new NoteEntry();
        note.setId(noteId);
        note.setViewCount(42L);
        when(noteEntryMapper.selectById(noteId)).thenReturn(note);

        assertEquals(42L, service.getViewCount(noteId),
                "浏览量缓存过期后应回源数据库，不能归零");
        verify(valueOps).set(eq("note:view:" + noteId), eq(42L), anyLong(), any(TimeUnit.class));
    }

    /**
     * 批量取互动数据应走 MGET，不能逐条查询。
     */
    @Test
    void batchGetEngagement_ShouldUseMultiGet() {
        service.batchGetEngagement(java.util.List.of(1L, 2L, 3L));

        verify(valueOps).multiGet(java.util.List.of("note:view:1", "note:view:2", "note:view:3"));
        verify(valueOps).multiGet(java.util.List.of("note:like:count:1", "note:like:count:2", "note:like:count:3"));
        // 逐条回源的老实现会走这个方法
        verify(noteLikeMapper, never()).countByNoteId(anyLong());
    }
}
