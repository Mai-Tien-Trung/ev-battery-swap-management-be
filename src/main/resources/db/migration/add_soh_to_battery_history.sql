-- Migration: Add soh column to battery_history table
-- Date: 2025-11-25
-- Description: Track State of Health (SoH) at the time of each battery event

ALTER TABLE battery_history 
ADD COLUMN soh DOUBLE PRECISION;

-- Comment
COMMENT ON COLUMN battery_history.soh IS 'State of Health (%) at the time of the event (0-100)';
