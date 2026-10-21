-- Migration: Persist note view counts
-- Description: 浏览量此前仅存于 Redis 且带 7 天 TTL，导致每 7 天静默归零。
--              增加持久化列，Redis 作为计数缓冲，由定时任务回写。

ALTER TABLE note_entry
    ADD COLUMN view_count BIGINT NOT NULL DEFAULT 0 COMMENT '累计浏览量';
