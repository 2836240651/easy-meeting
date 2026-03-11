-- Database migration script to add avatar field to user_info table
-- Execute this script in your MySQL database

-- Add avatar column to user_info table
ALTER TABLE user_info ADD COLUMN avatar VARCHAR(500) DEFAULT NULL COMMENT '头像URL';

-- Optional: Add index on avatar column if needed for search performance
-- CREATE INDEX idx_user_info_avatar ON user_info(avatar);

-- Verify the column was added successfully
-- DESCRIBE user_info;