-- Migration: Thêm cột used_count vào bảng reservations
-- Date: 2025-11-26
-- Description: Track số lượng pin đã swap trong reservation

-- Bước 1: Thêm column used_count (cho phép NULL tạm thời)
ALTER TABLE reservations 
ADD COLUMN IF NOT EXISTS used_count INTEGER;

-- Bước 2: Set giá trị mặc định cho các record hiện tại
-- Logic: Nếu status = USED → used_count = quantity (đã swap hết)
--        Nếu status khác → used_count = 0 (chưa swap)
UPDATE reservations
SET used_count = CASE
    WHEN status = 'USED' THEN quantity
    ELSE 0
END
WHERE used_count IS NULL;

-- Bước 3: Thêm constraint NOT NULL sau khi đã update dữ liệu
ALTER TABLE reservations 
ALTER COLUMN used_count SET NOT NULL;

-- Bước 4: Set default value cho records mới
ALTER TABLE reservations 
ALTER COLUMN used_count SET DEFAULT 0;

-- Verify migration
SELECT 
    id,
    status,
    quantity,
    used_count,
    reserved_at
FROM reservations
ORDER BY id DESC
LIMIT 10;
