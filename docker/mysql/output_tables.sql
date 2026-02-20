-- ============================================
-- Output 模块数据库表创建脚本
-- ============================================

USE knowledge_flow;

-- 1. 用户主页配置表
CREATE TABLE IF NOT EXISTS user_homepage (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    slug VARCHAR(50) COMMENT '个性化URL（如：ray-huang）',
    title VARCHAR(100) COMMENT '主页标题',
    bio TEXT COMMENT '个人简介',
    theme VARCHAR(20) DEFAULT 'default' COMMENT '主题：default, dark, light',
    layout VARCHAR(20) DEFAULT 'standard' COMMENT '布局：standard, compact, minimal',
    show_stats TINYINT DEFAULT 1 COMMENT '是否显示统计：0-否，1-是',
    show_skills TINYINT DEFAULT 1 COMMENT '是否显示技能：0-否，1-是',
    show_notes TINYINT DEFAULT 1 COMMENT '是否显示笔记：0-否，1-是',
    custom_css TEXT COMMENT '自定义CSS',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-正常，1-删除',
    UNIQUE KEY uk_user_id (user_id),
    UNIQUE KEY uk_slug (slug),
    INDEX idx_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户主页配置';

-- 2. 邀请码表
CREATE TABLE IF NOT EXISTS invite_code (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    code VARCHAR(32) NOT NULL COMMENT '邀请码',
    creator_id BIGINT NOT NULL COMMENT '创建者ID',
    used_by BIGINT COMMENT '使用者ID',
    status TINYINT DEFAULT 0 COMMENT '状态：0-未使用，1-已使用，2-已过期',
    expire_time DATETIME COMMENT '过期时间',
    used_time DATETIME COMMENT '使用时间',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-正常，1-删除',
    UNIQUE KEY uk_code (code),
    INDEX idx_creator (creator_id),
    INDEX idx_used_by (used_by),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邀请码表';

-- 3. 分享链接表
CREATE TABLE IF NOT EXISTS share_link (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    resource_type VARCHAR(20) NOT NULL COMMENT '资源类型：note, folder, collection',
    resource_id BIGINT NOT NULL COMMENT '资源ID',
    share_code VARCHAR(32) NOT NULL COMMENT '分享码',
    password VARCHAR(20) COMMENT '访问密码',
    expire_time DATETIME COMMENT '过期时间',
    view_count INT DEFAULT 0 COMMENT '查看次数',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-正常，1-删除',
    UNIQUE KEY uk_share_code (share_code),
    INDEX idx_user (user_id),
    INDEX idx_resource (resource_type, resource_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分享链接表';

-- 4. 访客记录表
CREATE TABLE IF NOT EXISTS visitor_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '被访问用户ID',
    visitor_ip VARCHAR(45) COMMENT '访客IP',
    visitor_ua TEXT COMMENT '访客UserAgent',
    referer VARCHAR(500) COMMENT '来源',
    resource_type VARCHAR(20) COMMENT '访问资源类型',
    resource_id BIGINT COMMENT '访问资源ID',
    visit_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
    INDEX idx_user_time (user_id, visit_time),
    INDEX idx_resource (resource_type, resource_id),
    INDEX idx_visit_time (visit_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='访客记录';

-- 完成标记
SELECT 'Output 模块数据库表创建成功！' AS Status;
