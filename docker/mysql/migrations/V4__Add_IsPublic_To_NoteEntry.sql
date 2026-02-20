-- Migration: Add is_public column to note_entry table
-- Date: 2026-01-26
-- Description: Add is_public field to support note visibility

ALTER TABLE note_entry
ADD COLUMN is_public TINYINT(1) DEFAULT 0 COMMENT '是否公开: 0-否, 1-是';
