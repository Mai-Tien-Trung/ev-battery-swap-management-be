-- Migration: Add swap_count to battery_serials table
-- Date: 2025-11-24
-- Description: Track the number of successful swaps for each battery

ALTER TABLE battery_serials 
ADD COLUMN swap_count INTEGER DEFAULT 0;

-- Update existing batteries to have swap_count = 0
UPDATE battery_serials 
SET swap_count = 0 
WHERE swap_count IS NULL;

-- Add comment
COMMENT ON COLUMN battery_serials.swap_count IS 'Number of successful battery swaps';
