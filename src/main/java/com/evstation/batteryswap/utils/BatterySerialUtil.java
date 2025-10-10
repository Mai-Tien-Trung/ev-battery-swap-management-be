package com.evstation.batteryswap.utils;

import java.util.UUID;

public class BatterySerialUtil {

    public static String generateSerialNumber() {
        return "BAT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
