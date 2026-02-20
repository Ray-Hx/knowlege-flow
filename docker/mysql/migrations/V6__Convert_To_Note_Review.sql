-- Migration: Convert spaced review from flashcard-based to note-based
-- Date: 2026-02-08
-- Description: Remove flashcard tables, modify spaced_review to use note_id

-- 1. Drop old flashcard tables
DROP TABLE IF EXISTS spaced_card;
DROP TABLE IF EXISTS spaced_log;

-- 2. Recreate spaced_review table for note-based review
DROP TABLE IF EXISTS spaced_review;
CREATE TABLE spaced_review (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    note_id BIGINT NOT NULL COMMENT '笔记ID',
    next_review_time DATETIME NOT NULL COMMENT '下次复习时间',
    interval_days INT DEFAULT 1 COMMENT '间隔天数',
    repetition_count INT DEFAULT 0 COMMENT '复习次数',
    easiness_factor DECIMAL(4,2) DEFAULT 2.50 COMMENT '难度因子(SM-2)',
    last_review_time DATETIME COMMENT '上次复习时间',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_note (user_id, note_id),
    INDEX idx_user_next_review (user_id, next_review_time),
    INDEX idx_note_id (note_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记复习调度表';

-- 3. Create note review questions cache table
CREATE TABLE IF NOT EXISTS note_review_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    note_id BIGINT NOT NULL COMMENT '笔记ID',
    question TEXT NOT NULL COMMENT 'AI生成的问题',
    answer TEXT NOT NULL COMMENT 'AI生成的参考答案',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_note_id (note_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记复习问题缓存表';
