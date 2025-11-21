package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.dto.response.VehicleModelResponse;
import com.evstation.batteryswap.entity.VehicleModel;
import com.evstation.batteryswap.repository.VehicleModelRepository;
import com.evstation.batteryswap.service.VehicleModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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
    public List<VehicleModelResponse> getAll() {
        return vehicleModelRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public VehicleModelResponse getById(Long id) {
        VehicleModel model = vehicleModelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle model not found"));
        return mapToResponse(model);
    }



    @Override
    public VehicleModelResponse update(Long id, VehicleModel model) {
        VehicleModel existing = vehicleModelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle model not found"));
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
        VehicleModel saved = vehicleModelRepository.save(existing);
        return mapToResponse(saved);    }

    @Override
    public void delete(Long id) {
        VehicleModel existing = vehicleModelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle model not found"));
        vehicleModelRepository.delete(existing);
    }
    private VehicleModelResponse mapToResponse(VehicleModel model) {
        return VehicleModelResponse.builder()
                .id(model.getId())
                .name(model.getName())
                .brand(model.getBrand())
                .wheelbase(model.getWheelbase())
                .groundClearance(model.getGroundClearance())
                .seatHeight(model.getSeatHeight())
                .frontTire(model.getFrontTire())
                .rearTire(model.getRearTire())
                .frontSuspension(model.getFrontSuspension())
                .rearSuspension(model.getRearSuspension())
                .brakeSystem(model.getBrakeSystem())
                .trunkCapacity(model.getTrunkCapacity())
                .weightWithoutBattery(model.getWeightWithoutBattery())
                .weightWithBattery(model.getWeightWithBattery())
                .build();
    }

}
