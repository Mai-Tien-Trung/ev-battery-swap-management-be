package com.evstation.batteryswap.enums;

public enum BatteryStatus {
    AVAILABLE,   // còn tốt, sẵn sàng đổi
    IN_USE,      // đang gắn trong xe
    PENDING_OUT,       // pin cũ vừa tháo ra, chờ xác nhận
    PENDING_IN,        // pin mới tạm cấp cho xe, chờ xác nhận
    DAMAGED,     // bị hỏng
    MAINTENANCE  // đang bảo trì
}
