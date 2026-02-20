package com.ray.knowledgeflow.module.note.service;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 笔记互动服务（浏览量、点赞）
 *
 * @author Ray
 * @since 2026-02-08
 */
public interface NoteEngagementService {

    /**
     * 增加浏览量
     */
    void incrementViewCount(Long noteId);

    /**
     * 获取浏览量
     */
    Long getViewCount(Long noteId);

    /**
     * 点赞/取消点赞
     * 
     * @return 操作后的点赞状态和数量
     */
    LikeResult toggleLike(Long userId, Long noteId);

    /**
     * 获取点赞数
     */
    Long getLikeCount(Long noteId);

    /**
     * 判断用户是否已点赞
     */
    boolean isLiked(Long userId, Long noteId);

    /**
     * 批量获取笔记的互动数据
     */
    Map<Long, EngagementStats> batchGetEngagement(List<Long> noteIds);

    /**
     * 点赞结果
     */
    @Data
    class LikeResult {
        private boolean liked;
        private Long likeCount;

        public LikeResult(boolean liked, Long likeCount) {
            this.liked = liked;
            this.likeCount = likeCount;
        }
    }

    /**
     * 互动统计
     */
    @Data
    class EngagementStats {
        private Long viewCount;
        private Long likeCount;

        public EngagementStats(Long viewCount, Long likeCount) {
            this.viewCount = viewCount;
            this.likeCount = likeCount;
        }
    }
}
