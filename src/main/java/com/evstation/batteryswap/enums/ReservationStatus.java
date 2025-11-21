package com.evstation.batteryswap.enums;

/**
 * Trạng thái của Reservation (đặt trước pin)
 * 
 * Flow: ACTIVE → USED (nếu swap thành công)
 *              → EXPIRED (nếu hết hạn 1 giờ)
 *              → CANCELLED (nếu user hủy)
 */
public enum ReservationStatus {
    /**
     * Đang giữ pin, chưa hết hạn (trong vòng 1 giờ)
     * - Pin đã được lock (status = RESERVED)
     * - User có thể đến swap bất kỳ lúc nào trong 1 giờ
     */
    ACTIVE,
    
    /**
     * Đã sử dụng - Swap thành công
     * - Pin đã được đổi (swap transaction completed)
     * - Reservation đã hoàn thành mục đích
     */
    USED,
    
    /**
     * Hết hạn - Quá 1 giờ chưa đến swap
     * - Pin tự động trả về AVAILABLE (auto-expire job)
     * - User không thể sử dụng reservation này nữa
     */
    EXPIRED,
    
    /**
     * Đã hủy - User chủ động hủy hoặc admin hủy
     * - Pin tự động trả về AVAILABLE
     * - Có thể có lý do hủy (cancel_reason)
     */
    CANCELLED
}
