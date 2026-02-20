CREATE TABLE IF NOT EXISTS `ai_analysis` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `target_type` varchar(20) NOT NULL COMMENT '目标类型 (NOTE, CONCEPT, SOURCE)',
  `target_id` bigint(20) NOT NULL COMMENT '目标ID',
  `content` text COMMENT '解析内容',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_target` (`target_type`, `target_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI深度解析记录';
