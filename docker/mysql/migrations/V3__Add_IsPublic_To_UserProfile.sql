-- Migration: Add is_public column to sys_user_profile table
-- Date: 2026-01-26
-- Description: Add is_public field to support public/private profile visibility

ALTER TABLE sys_user_profile
ADD COLUMN is_public TINYINT(1) DEFAULT 0 COMMENT '是否公开个人主页: 0-否, 1-是';
