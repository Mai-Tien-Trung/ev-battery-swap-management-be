-- Migration: Update invoices table to support subscription renewal invoices
-- Date: 2025-11-07
-- Description: 
--   - Make swap_transaction_id nullable (not all invoices are from swaps)
--   - Add invoice_type column to distinguish SWAP_OVERAGE vs SUBSCRIPTION_RENEWAL
--   - Make usage_type, overage, rate nullable (only used for SWAP_OVERAGE)

-- 1. Remove NOT NULL constraint from swap_transaction_id
ALTER TABLE invoices 
    ALTER COLUMN swap_transaction_id DROP NOT NULL;

-- 2. Add invoice_type column
ALTER TABLE invoices
    ADD COLUMN invoice_type VARCHAR(50);

-- 3. Update existing invoices to SWAP_OVERAGE type
UPDATE invoices 
SET invoice_type = 'SWAP_OVERAGE'
WHERE swap_transaction_id IS NOT NULL;

-- 4. Make usage_type nullable
ALTER TABLE invoices
    ALTER COLUMN usage_type DROP NOT NULL;

-- 5. Make overage nullable
ALTER TABLE invoices
    ALTER COLUMN overage DROP NOT NULL;

-- 6. Make rate nullable
ALTER TABLE invoices
    ALTER COLUMN rate DROP NOT NULL;

-- Verify migration
SELECT 
    id,
    invoice_type,
    swap_transaction_id,
    subscription_id,
    amount,
    status,
    description
FROM invoices
ORDER BY created_at DESC;

-- Expected results:
-- - Existing invoices should have invoice_type = 'SWAP_OVERAGE'
-- - swap_transaction_id can be NULL for future renewal invoices
-- - usage_type, overage, rate can be NULL for renewal invoices
