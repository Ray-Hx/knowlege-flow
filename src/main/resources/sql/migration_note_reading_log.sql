-- 添加笔记阅读时长追踪表
-- 执行此脚本以支持笔记阅读时间统计功能

CREATE TABLE IF NOT EXISTS `note_reading_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `note_id` bigint NOT NULL COMMENT '笔记ID',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `duration_seconds` int DEFAULT NULL COMMENT '持续时长(秒)',
  `is_active` tinyint(1) DEFAULT 1 COMMENT '是否活跃状态',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_note_time` (`user_id`, `note_id`, `start_time`),
  KEY `idx_user_time` (`user_id`, `start_time`),
  KEY `idx_user_active` (`user_id`, `is_active`),
  CONSTRAINT `fk_reading_note` FOREIGN KEY (`note_id`) REFERENCES `note_entry` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记阅读时长记录表';
