-- =====================================================
-- Migration: Create reservation tables
-- Date: 2025-11-20
-- Purpose: Battery reservation feature
-- =====================================================

-- Create reservations table
CREATE TABLE IF NOT EXISTS reservations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    vehicle_id BIGINT NOT NULL,
    station_id BIGINT NOT NULL,
    subscription_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    quantity INT NOT NULL,
    reserved_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expire_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    swap_transaction_id BIGINT,
    cancelled_at TIMESTAMP,
    cancel_reason VARCHAR(255),
    
    CONSTRAINT reservations_user_fk FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT reservations_vehicle_fk FOREIGN KEY (vehicle_id) 
        REFERENCES vehicles(id) ON DELETE CASCADE,
    CONSTRAINT reservations_station_fk FOREIGN KEY (station_id) 
        REFERENCES stations(id) ON DELETE CASCADE,
    CONSTRAINT reservations_subscription_fk FOREIGN KEY (subscription_id) 
        REFERENCES subscriptions(id) ON DELETE CASCADE,
    CONSTRAINT reservations_swap_transaction_fk FOREIGN KEY (swap_transaction_id) 
        REFERENCES swap_transactions(id) ON DELETE SET NULL,
    CONSTRAINT reservations_status_check CHECK (
        status IN ('ACTIVE', 'USED', 'EXPIRED', 'CANCELLED')
    )
);

-- Create reservation_items table (junction table)
CREATE TABLE IF NOT EXISTS reservation_items (
    id BIGSERIAL PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    battery_serial_id BIGINT NOT NULL,
    
    CONSTRAINT reservation_items_reservation_fk FOREIGN KEY (reservation_id) 
        REFERENCES reservations(id) ON DELETE CASCADE,
    CONSTRAINT reservation_items_battery_fk FOREIGN KEY (battery_serial_id) 
        REFERENCES battery_serials(id) ON DELETE CASCADE,
    CONSTRAINT reservation_items_unique UNIQUE (reservation_id, battery_serial_id)
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_reservations_user_vehicle_status 
    ON reservations(user_id, vehicle_id, status);

CREATE INDEX IF NOT EXISTS idx_reservations_status_expire 
    ON reservations(status, expire_at);

CREATE INDEX IF NOT EXISTS idx_reservations_user_station_status 
    ON reservations(user_id, vehicle_id, station_id, status);

CREATE INDEX IF NOT EXISTS idx_reservation_items_reservation 
    ON reservation_items(reservation_id);

CREATE INDEX IF NOT EXISTS idx_reservation_items_battery 
    ON reservation_items(battery_serial_id);

-- Verify tables created
SELECT 
    table_name,
    (SELECT COUNT(*) FROM information_schema.columns WHERE table_name = t.table_name) AS column_count
FROM information_schema.tables t
WHERE table_name IN ('reservations', 'reservation_items')
  AND table_schema = 'public';
