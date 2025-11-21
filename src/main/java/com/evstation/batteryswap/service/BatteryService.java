package com.evstation.batteryswap.service;

import com.evstation.batteryswap.entity.Battery;

import java.util.List;

public interface BatteryService {

    List<Battery> getAll();

    Battery getById(Long id);

    Battery create(Battery battery);

    Battery update(Long id, Battery battery);

    void delete(Long id);
}
