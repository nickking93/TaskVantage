-- Migration: Add isAdmin flag to users table
-- This enables role-based access control for administrative operations

ALTER TABLE users ADD COLUMN is_admin BOOLEAN NOT NULL DEFAULT FALSE;

-- Optional: Grant admin privileges to a specific user
-- Uncomment and update the username below to set an admin user
-- UPDATE users SET is_admin = TRUE WHERE username = 'your_admin_username';
