-- Debug transaction 41 và battery options

-- 1. Chi tiết transaction 41
SELECT 
    st.id,
    st.status,
    st.user_id,
    u.username,
    st.vehicle_id,
    st.station_id,
    sta.name AS station_name,
    st.reservation_id,
    st.timestamp
FROM swap_transactions st
JOIN users u ON st.user_id = u.id
JOIN stations sta ON st.station_id = sta.id
WHERE st.id = 41;

-- 2. Nếu có reservation → Xem batteries đã đặt
SELECT 
    r.id AS reservation_id,
    r.status AS reservation_status,
    r.quantity,
    r.used_count,
    ri.id AS item_id,
    bs.id AS battery_id,
    bs.serial_number,
    bs.status AS battery_status,
    bs.charge_percent,
    bs.state_of_health
FROM reservations r
JOIN reservation_items ri ON r.id = ri.reservation_id
JOIN battery_serials bs ON ri.battery_serial_id = bs.id
WHERE r.id IN (
    SELECT reservation_id 
    FROM swap_transactions 
    WHERE id = 41
);

-- 3. Xem battery 8
SELECT 
    id,
    serial_number,
    status,
    charge_percent,
    state_of_health,
    station_id,
    vehicle_id
FROM battery_serials
WHERE id = 8;

-- 4. Xem tất cả batteries AVAILABLE tại station của transaction 41
SELECT 
    bs.id,
    bs.serial_number,
    bs.status,
    bs.charge_percent,
    bs.state_of_health,
    bs.station_id
FROM battery_serials bs
WHERE bs.station_id = (
    SELECT station_id 
    FROM swap_transactions 
    WHERE id = 41
)
AND bs.status = 'AVAILABLE'
AND bs.charge_percent >= 95
ORDER BY bs.charge_percent DESC, bs.state_of_health DESC;

-- 5. Solution options:

-- Option A: Nếu battery 8 đang RESERVED nhưng không thuộc reservation
--           → Chọn battery khác từ list AVAILABLE ở query 4

-- Option B: Nếu transaction CÓ reservation và battery 8 trong reservation
--           → Release battery 8 về AVAILABLE (do có thể bị lỗi)
-- UPDATE battery_serials SET status = 'RESERVED' WHERE id = 8;

-- Option C: Nếu transaction KHÔNG CÓ reservation (walk-in)
--           → Phải chọn battery AVAILABLE từ query 4
