-- V3: Add role column to users table for ROLE_ADMIN support
-- Default all existing users to ROLE_USER

ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(32) NOT NULL DEFAULT 'ROLE_USER';
