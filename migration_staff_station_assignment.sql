-- Migration: Add assigned_station_id to users table
-- This allows STAFF users to be assigned to a specific station

ALTER TABLE users 
ADD COLUMN assigned_station_id BIGINT;

-- Add foreign key constraint
ALTER TABLE users
ADD CONSTRAINT fk_user_assigned_station 
    FOREIGN KEY (assigned_station_id) REFERENCES stations(id)
    ON DELETE SET NULL;

-- Create index for better query performance
CREATE INDEX idx_users_assigned_station ON users(assigned_station_id);

-- Add comment for documentation
COMMENT ON COLUMN users.assigned_station_id IS 'Station assigned to STAFF users. NULL for USER and ADMIN roles.';
