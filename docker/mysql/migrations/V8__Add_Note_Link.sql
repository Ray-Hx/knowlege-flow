-- Migration: Persist stable note-to-note links
-- Description: Markdown 中的显式引用以稳定 note_id 解析后写入关系表。
--              MySQL 是双向链接的事实来源，Neo4j 仅作为图遍历投影。

CREATE TABLE IF NOT EXISTS note_link (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '关系所属用户（source note owner）',
    source_note_id BIGINT NOT NULL COMMENT '引用发起笔记ID',
    target_note_id BIGINT NOT NULL COMMENT '被引用笔记ID',
    link_type VARCHAR(32) NOT NULL DEFAULT 'EXPLICIT' COMMENT 'EXPLICIT/AI_RELATED',
    label VARCHAR(64) DEFAULT NULL COMMENT '可选关系标签',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_note_link (source_note_id, target_note_id, link_type),
    INDEX idx_note_link_user (user_id),
    INDEX idx_note_link_source (source_note_id, link_type),
    INDEX idx_note_link_target (target_note_id, link_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记间稳定关系';
