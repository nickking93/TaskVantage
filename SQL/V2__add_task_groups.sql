-- Migration: Add Task Groups feature
-- This migration adds support for organizing tasks into groups (Kanban columns)

-- Create task_groups table
CREATE TABLE IF NOT EXISTS task_groups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    color VARCHAR(50),
    display_order INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task_group_user_id (user_id),
    CONSTRAINT fk_task_group_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Add group_id column to tasks table
ALTER TABLE tasks
ADD COLUMN group_id BIGINT NULL,
ADD INDEX idx_task_group_id (group_id),
ADD CONSTRAINT fk_task_group FOREIGN KEY (group_id) REFERENCES task_groups(id) ON DELETE SET NULL;
