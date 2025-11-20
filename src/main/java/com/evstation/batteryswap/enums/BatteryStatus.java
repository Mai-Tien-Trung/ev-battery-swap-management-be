package com.evstation.batteryswap.enums;

/**
 * Trạng thái của pin trong hệ thống
 */
public enum BatteryStatus {
    AVAILABLE,   // Còn tốt, sẵn sàng đổi hoặc đặt trước
    IN_USE,      // Đang gắn trong xe
    RESERVED,    // Đã được đặt trước, giữ trong 1 giờ (NEW - cho reservation)
    PENDING_OUT, // Pin cũ vừa tháo ra, chờ staff xác nhận
    PENDING_IN,  // Pin mới chờ staff xác nhận
    DAMAGED,     // Bị hỏng, không sử dụng được
    MAINTENANCE  // Đang bảo trì, SoH < 80%
}
