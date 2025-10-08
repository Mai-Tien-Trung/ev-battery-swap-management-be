package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.entity.Battery;
import com.evstation.batteryswap.repository.BatteryRepository;
import com.evstation.batteryswap.service.BatteryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BatteryServiceImpl implements BatteryService {

    private final BatteryRepository batteryRepository;

    public BatteryServiceImpl(BatteryRepository batteryRepository) {
        this.batteryRepository = batteryRepository;
    }

    public List<Battery> getAll() {
        return batteryRepository.findAll();
    }

    public Battery getById(Long id) {
        return batteryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Battery not found"));
    }

    public Battery create(Battery battery) {
        return batteryRepository.save(battery);
    }

    public Battery update(Long id, Battery newBattery) {
        Battery old = getById(id);
        old.setName(newBattery.getName());
        old.setType(newBattery.getType());
        old.setDesignCapacity(newBattery.getDesignCapacity());
        old.setDescription(newBattery.getDescription());
        return batteryRepository.save(old);
    }

    public void delete(Long id) {
        batteryRepository.deleteById(id);
    }
}
