package com.evstation.batteryswap.enums;

public enum BatteryEventType {
    CREATED, // Battery serial created
    STATUS_CHANGED, // Status changed (e.g., AVAILABLE → IN_USE)
    STATION_CHANGED, // Transferred between stations
    VEHICLE_ASSIGNED, // Assigned to a vehicle
    VEHICLE_RETURNED, // Returned from vehicle to station
    SOH_UPDATED, // State of Health manually updated by admin
    SWAPPED, // Used in a successful swap transaction
    TRANSFERRED // Admin transferred between stations
}
