package com.evstation.batteryswap.enums;

public enum BatteryStatus {
    AVAILABLE,   // còn tốt, sẵn sàng đổi
    IN_USE,      // đang gắn trong xe
    PENDING_OUT,       // pin cũ vừa tháo ra, chờ xác nhận
    PENDING_IN,        // pin mới  chờ xác nhận
    DAMAGED,     // bị hỏng
    MAINTENANCE,
    RESERVED
}
