/*
 * KnowledgeFlow 数据库结构脚本
 * 版本: 1.0.0
 * 描述: 用户、内容、笔记、学习与系统模块的初始数据库结构
 */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =================================================================
-- 1. 系统模块 (用户, 角色, 认证)
-- =================================================================

-- 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(64) NOT NULL COMMENT '用户名',
  `password` varchar(128) NOT NULL COMMENT '加密密码',
  `email` varchar(128) DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态: 1-启用, 0-禁用',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-正常, 1-删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- 用户扩展信息表
CREATE TABLE IF NOT EXISTS `sys_user_profile` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `bio` varchar(500) DEFAULT NULL COMMENT '个人简介',
  `github_id` varchar(64) DEFAULT NULL COMMENT 'GitHub ID',
  `github_token` varchar(255) DEFAULT NULL COMMENT 'GitHub Personal Access Token (加密)',
  `preferences` json DEFAULT NULL COMMENT '用户偏好设置 (JSON)',
  `learning_goals` json DEFAULT NULL COMMENT '学习目标 (JSON)',
  `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户扩展信息表';


-- 角色表
CREATE TABLE IF NOT EXISTS `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_code` varchar(32) NOT NULL COMMENT '角色编码 (ROLE_USER, ROLE_ADMIN)',
  `role_name` varchar(32) NOT NULL COMMENT '角色名称',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- =================================================================
-- 2. 内容模块 (聚合)
-- =================================================================

-- 内容源表 (RSS, API)
CREATE TABLE IF NOT EXISTS `content_source` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `type` varchar(32) NOT NULL COMMENT '源类型: RSS, GITHUB, ARXIV 等',
  `name` varchar(128) NOT NULL COMMENT '源名称',
  `url` varchar(512) NOT NULL COMMENT '源URL',
  `config` json DEFAULT NULL COMMENT '额外配置参数 (JSON格式)',
  `sync_interval` int DEFAULT 60 COMMENT '同步间隔 (分钟)',
  `last_sync_time` datetime DEFAULT NULL COMMENT '上次同步时间',
  `status` tinyint DEFAULT 1 COMMENT '状态: 1-启用, 0-禁用',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容源表';

-- 内容分类表
CREATE TABLE IF NOT EXISTS `content_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `name` varchar(64) NOT NULL COMMENT '分类名称',
  `parent_id` bigint DEFAULT 0 COMMENT '父分类ID',
  `sort_order` int DEFAULT 0 COMMENT '排序',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_parent` (`user_id`, `parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容分类表';

-- 聚合内容项表
CREATE TABLE IF NOT EXISTS `content_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `source_id` bigint DEFAULT NULL COMMENT '来源ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `title` varchar(255) NOT NULL COMMENT '内容标题',
  `url` varchar(512) NOT NULL COMMENT '原始URL',
  `author` varchar(128) DEFAULT NULL COMMENT '作者',
  `publish_time` datetime DEFAULT NULL COMMENT '原始发布时间',
  `summary` text COMMENT 'AI生成的摘要',
  `content_text` longtext COMMENT '提取的正文内容',
  `tags` json DEFAULT NULL COMMENT '提取的标签 (JSON)',
  `status` tinyint DEFAULT 0 COMMENT '状态: 0-未读, 1-已读, 2-收藏, 3-归档',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
  `deleted` tinyint DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_status` (`user_id`, `status`),
  KEY `idx_source` (`source_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聚合内容项表';

-- =================================================================
-- 3. 笔记模块
-- =================================================================

-- 笔记文件夹表
CREATE TABLE IF NOT EXISTS `note_folder` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `name` varchar(64) NOT NULL COMMENT '文件夹名称',
  `parent_id` bigint DEFAULT 0 COMMENT '父文件夹ID',
  `sort_order` int DEFAULT 0 COMMENT '排序',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_folder` (`user_id`, `parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记文件夹表';

-- 笔记表
CREATE TABLE IF NOT EXISTS `note_entry` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `folder_id` bigint DEFAULT NULL COMMENT '文件夹ID',
  `title` varchar(255) NOT NULL DEFAULT '无标题' COMMENT '笔记标题',
  `content_md` longtext COMMENT 'Markdown内容',
  `summary` varchar(1024) DEFAULT NULL COMMENT '简短摘要',
  `type` varchar(32) DEFAULT 'NOTE' COMMENT '类型: NOTE(笔记), ARTICLE(文章), SNIPPET(片段)',
  `status` tinyint DEFAULT 1 COMMENT '状态: 1-草稿, 2-发布',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_folder_note` (`user_id`, `folder_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记表';

-- 笔记附件表
CREATE TABLE IF NOT EXISTS `note_attachment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `note_id` bigint DEFAULT NULL COMMENT '关联笔记ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `file_name` varchar(255) NOT NULL COMMENT '文件名',
  `url` varchar(512) NOT NULL COMMENT '存储URL',
  `file_type` varchar(64) DEFAULT NULL COMMENT '文件类型',
  `file_size` bigint DEFAULT 0 COMMENT '文件大小(字节)',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`id`),
  KEY `idx_note_id` (`note_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记附件表';

-- 笔记版本历史表
CREATE TABLE IF NOT EXISTS `note_version` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `note_id` bigint NOT NULL COMMENT '笔记ID',
  `title` varchar(255) DEFAULT NULL COMMENT '标题快照',
  `content_md` longtext COMMENT '内容快照',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '版本时间',
  PRIMARY KEY (`id`),
  KEY `idx_note_history` (`note_id`, `created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记版本历史表';

-- =================================================================
-- 4. 学习模块 (任务 & 计划)
-- =================================================================

-- 学习任务表
CREATE TABLE IF NOT EXISTS `learning_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `title` varchar(255) NOT NULL COMMENT '任务标题',
  `description` text COMMENT '任务描述',
  `due_date` datetime DEFAULT NULL COMMENT '截止日期',
  `priority` tinyint DEFAULT 2 COMMENT '优先级: 1-高, 2-中, 3-低',
  `status` tinyint DEFAULT 0 COMMENT '状态: 0-待办, 1-进行中, 2-完成',
  `related_note_id` bigint DEFAULT NULL COMMENT '关联笔记ID',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `completed_time` datetime DEFAULT NULL COMMENT '完成时间',
  `deleted` tinyint DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_status_task` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习任务表';

-- 番茄钟记录表
CREATE TABLE IF NOT EXISTS `learning_pomodoro` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `task_id` bigint DEFAULT NULL COMMENT '关联任务ID',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `duration_minutes` int NOT NULL COMMENT '时长(分钟)',
  `status` tinyint DEFAULT 1 COMMENT '状态: 1-完成, 0-中断',
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='番茄钟记录表';

-- 学习计划表
CREATE TABLE IF NOT EXISTS `learning_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `title` varchar(255) NOT NULL COMMENT '计划标题',
  `description` text COMMENT '计划描述',
  `start_date` date DEFAULT NULL COMMENT '开始日期',
  `end_date` date DEFAULT NULL COMMENT '结束日期',
  `status` tinyint DEFAULT 0 COMMENT '状态: 0-计划中, 1-进行中, 2-已完成',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_plan` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习计划表';

-- =================================================================
-- 5. 间隔重复模块 (闪卡)
-- =================================================================

-- 笔记阅读时长记录表
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

-- =================================================================
-- 6. 间隔重复模块 (闪卡)
-- =================================================================

-- 闪卡表
CREATE TABLE IF NOT EXISTS `spaced_card` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `note_id` bigint DEFAULT NULL COMMENT '关联笔记ID',
  `front_content` text NOT NULL COMMENT '正面(问题)',
  `back_content` text NOT NULL COMMENT '背面(答案)',
  `tags` json DEFAULT NULL COMMENT '标签',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_card` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='记忆闪卡表';

-- 复习状态表 (SM-2算法)
CREATE TABLE IF NOT EXISTS `spaced_review` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `card_id` bigint NOT NULL COMMENT '闪卡ID',
  `next_review_time` datetime NOT NULL COMMENT '下次复习时间',
  `interval_days` int DEFAULT 0 COMMENT '间隔天数',
  `repetition_count` int DEFAULT 0 COMMENT '重复次数',
  `easiness_factor` decimal(5,2) DEFAULT 2.50 COMMENT '难度因子(EF)',
  `last_review_time` datetime DEFAULT NULL COMMENT '上次复习时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_card_id` (`card_id`),
  KEY `idx_next_review` (`next_review_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='复习状态表';

-- 复习日志表
CREATE TABLE IF NOT EXISTS `spaced_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `card_id` bigint NOT NULL COMMENT '闪卡ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `review_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '复习时间',
  `quality` tinyint NOT NULL COMMENT '质量评分(0-5)',
  `duration_ms` int DEFAULT 0 COMMENT '耗时(毫秒)',
  PRIMARY KEY (`id`),
  KEY `idx_card_log` (`card_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='复习日志表';

-- =================================================================
-- 6. AI 助手模块 (对话 & 消息)
-- =================================================================

-- AI 对话表
CREATE TABLE IF NOT EXISTS `ai_conversation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `title` varchar(200) DEFAULT NULL COMMENT '对话标题',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_ai` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话表';

-- AI 消息表
CREATE TABLE IF NOT EXISTS `ai_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `conversation_id` bigint NOT NULL COMMENT '对话ID',
  `role` varchar(20) NOT NULL COMMENT '角色(user/assistant)',
  `content` text NOT NULL COMMENT '消息内容',
  `references` json DEFAULT NULL COMMENT '引用来源(JSON)',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_conv_msg` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI消息表';

-- =================================================================
-- 7. 社区互动模块 (点赞 & 评论)
-- =================================================================

-- 笔记点赞表
CREATE TABLE IF NOT EXISTS `note_like` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `note_id` bigint NOT NULL COMMENT '笔记ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_note_user` (`note_id`, `user_id`),
  KEY `idx_note_id` (`note_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记点赞表';

-- 笔记评论表
CREATE TABLE IF NOT EXISTS `note_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `note_id` bigint NOT NULL COMMENT '笔记ID',
  `user_id` bigint NOT NULL COMMENT '评论者用户ID',
  `parent_id` bigint DEFAULT NULL COMMENT '父评论ID（顶层评论为NULL）',
  `content` text NOT NULL COMMENT '评论内容',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_note_id` (`note_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记评论表';

SET FOREIGN_KEY_CHECKS = 1;

