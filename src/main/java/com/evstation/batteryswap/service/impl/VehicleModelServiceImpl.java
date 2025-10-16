package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.entity.VehicleModel;
import com.evstation.batteryswap.repository.VehicleModelRepository;
import com.evstation.batteryswap.service.VehicleModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleModelServiceImpl implements VehicleModelService {

    private final VehicleModelRepository vehicleModelRepository;

    @Override
    public VehicleModel create(VehicleModel model) {
        if (vehicleModelRepository.existsByName(model.getName())) {
            throw new RuntimeException("Vehicle model already exists");
        }
        return vehicleModelRepository.save(model);
    }

    @Override
    public List<VehicleModel> getAll() {
        return vehicleModelRepository.findAll();
    }

    @Override
    public VehicleModel getById(Long id) {
        return vehicleModelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle model not found"));
    }

    @Override
    public VehicleModel update(Long id, VehicleModel model) {
        VehicleModel existing = getById(id);
        existing.setBrand(model.getBrand());
        existing.setWheelbase(model.getWheelbase());
        existing.setGroundClearance(model.getGroundClearance());
        existing.setSeatHeight(model.getSeatHeight());
        existing.setFrontTire(model.getFrontTire());
        existing.setRearTire(model.getRearTire());
        existing.setFrontSuspension(model.getFrontSuspension());
        existing.setRearSuspension(model.getRearSuspension());
        existing.setBrakeSystem(model.getBrakeSystem());
        existing.setTrunkCapacity(model.getTrunkCapacity());
        existing.setWeightWithoutBattery(model.getWeightWithoutBattery());
        existing.setWeightWithBattery(model.getWeightWithBattery());
        return vehicleModelRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        VehicleModel model = getById(id);
        vehicleModelRepository.delete(model);
    }
}
