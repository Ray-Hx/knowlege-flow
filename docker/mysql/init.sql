-- ============================================
-- KnowledgeFlow 数据库初始化脚本
-- ============================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS knowledge_flow 
  CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;

-- 创建应用用户
CREATE USER IF NOT EXISTS 'kflow'@'%' IDENTIFIED BY 'kflow_password';

-- 授权
GRANT ALL PRIVILEGES ON knowledge_flow.* TO 'kflow'@'%';
GRANT SELECT, SHOW VIEW ON *.* TO 'kflow'@'%';

-- 刷新权限
FLUSH PRIVILEGES;

USE knowledge_flow;

-- 初始化完成标记
SELECT 'KnowledgeFlow Database Initialized Successfully!' AS Status;
