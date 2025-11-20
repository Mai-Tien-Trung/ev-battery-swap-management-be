-- =====================================================
-- Migration: Add RESERVED status to battery_serials
-- Date: 2025-11-20
-- Purpose: Support battery reservation feature
-- =====================================================

-- Step 1: Drop existing constraint (if exists)
ALTER TABLE battery_serials 
DROP CONSTRAINT IF EXISTS battery_serials_status_check;

-- Step 2: Add new constraint with RESERVED status
ALTER TABLE battery_serials 
ADD CONSTRAINT battery_serials_status_check 
CHECK (status IN (
    'AVAILABLE',
    'RESERVED',      -- NEW: For reservation feature
    'IN_USE',
    'MAINTENANCE',
    'RETIRED',
    'PENDING_IN',
    'PENDING_OUT'
));

-- Verify constraint
SELECT 
    con.conname AS constraint_name,
    pg_get_constraintdef(con.oid) AS constraint_definition
FROM pg_constraint con
JOIN pg_class rel ON rel.oid = con.conrelid
WHERE rel.relname = 'battery_serials' 
  AND con.conname = 'battery_serials_status_check';
