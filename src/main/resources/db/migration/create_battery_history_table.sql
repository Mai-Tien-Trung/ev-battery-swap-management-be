-- Migration: Create battery_history table
-- Date: 2025-11-24
-- Description: Track complete lifecycle of each battery

CREATE TABLE battery_history (
    id BIGSERIAL PRIMARY KEY,
    battery_serial_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    station_id BIGINT,
    vehicle_id BIGINT,
    performed_by_user_id BIGINT,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_battery_history_battery_serial 
        FOREIGN KEY (battery_serial_id) 
        REFERENCES battery_serials(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_battery_history_station 
        FOREIGN KEY (station_id) 
        REFERENCES stations(id) 
        ON DELETE SET NULL,
    
    CONSTRAINT fk_battery_history_vehicle 
        FOREIGN KEY (vehicle_id) 
        REFERENCES vehicles(id) 
        ON DELETE SET NULL,
    
    CONSTRAINT fk_battery_history_user 
        FOREIGN KEY (performed_by_user_id) 
        REFERENCES users(id) 
        ON DELETE SET NULL
);

-- Indexes for better query performance
CREATE INDEX idx_battery_history_battery_serial_id ON battery_history(battery_serial_id);
CREATE INDEX idx_battery_history_event_type ON battery_history(event_type);
CREATE INDEX idx_battery_history_created_at ON battery_history(created_at DESC);

-- Comments
COMMENT ON TABLE battery_history IS 'Complete audit trail for battery lifecycle events';
COMMENT ON COLUMN battery_history.event_type IS 'Type of event: CREATED, STATUS_CHANGED, SWAPPED, TRANSFERRED, etc.';
COMMENT ON COLUMN battery_history.old_value IS 'Previous value before change (JSON or text)';
COMMENT ON COLUMN battery_history.new_value IS 'New value after change (JSON or text)';
