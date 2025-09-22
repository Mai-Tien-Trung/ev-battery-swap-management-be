package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.dto.request.VehicleRequest;
import com.evstation.batteryswap.dto.response.VehicleResponse;
import com.evstation.batteryswap.entity.Vehicle;
import com.evstation.batteryswap.repository.VehicleRepository;
import com.evstation.batteryswap.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VehicleServiceImpl implements VehicleService {

    @Autowired
    private VehicleRepository vehicleRepository;

    // 🔹 Convert Entity -> DTO
    private VehicleResponse mapToResponse(Vehicle vehicle) {
        VehicleResponse res = new VehicleResponse();
        res.setId(vehicle.getId());
        res.setVin(vehicle.getVin());
        res.setModel(vehicle.getModel());
        res.setWheelbase(vehicle.getWheelbase());
        res.setGroundClearance(vehicle.getGroundClearance());
        res.setSeatHeight(vehicle.getSeatHeight());
        res.setFrontTire(vehicle.getFrontTire());
        res.setRearTire(vehicle.getRearTire());
        res.setFrontSuspension(vehicle.getFrontSuspension());
        res.setRearSuspension(vehicle.getRearSuspension());
        res.setBrakeSystem(vehicle.getBrakeSystem());
        res.setTrunkCapacity(vehicle.getTrunkCapacity());
        res.setWeightWithoutBattery(vehicle.getWeightWithoutBattery());
        res.setWeightWithBattery(vehicle.getWeightWithBattery());
        return res;
    }

    // 🔹 Convert DTO -> Entity
    private Vehicle mapToEntity(VehicleRequest request) {
        Vehicle vehicle = new Vehicle();
        vehicle.setVin(request.getVin());
        vehicle.setModel(request.getModel());
        vehicle.setWheelbase(request.getWheelbase());
        vehicle.setGroundClearance(request.getGroundClearance());
        vehicle.setSeatHeight(request.getSeatHeight());
        vehicle.setFrontTire(request.getFrontTire());
        vehicle.setRearTire(request.getRearTire());
        vehicle.setFrontSuspension(request.getFrontSuspension());
        vehicle.setRearSuspension(request.getRearSuspension());
        vehicle.setBrakeSystem(request.getBrakeSystem());
        vehicle.setTrunkCapacity(request.getTrunkCapacity());
        vehicle.setWeightWithoutBattery(request.getWeightWithoutBattery());
        vehicle.setWeightWithBattery(request.getWeightWithBattery());
        return vehicle;
    }

    @Override
    public VehicleResponse createVehicle(VehicleRequest request) {
        Vehicle vehicle = mapToEntity(request);
        // 🚫 Không set user ở đây, vì Admin tạo xe mới chưa gán user
        return mapToResponse(vehicleRepository.save(vehicle));
    }

    @Override
    public List<VehicleResponse> getAllVehicles() {
        return vehicleRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public VehicleResponse getVehicleById(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        return mapToResponse(vehicle);
    }

    @Override
    public VehicleResponse updateVehicle(Long id, VehicleRequest request) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        vehicle.setVin(request.getVin());
        vehicle.setModel(request.getModel());
        vehicle.setWheelbase(request.getWheelbase());
        vehicle.setGroundClearance(request.getGroundClearance());
        vehicle.setSeatHeight(request.getSeatHeight());
        vehicle.setFrontTire(request.getFrontTire());
        vehicle.setRearTire(request.getRearTire());
        vehicle.setFrontSuspension(request.getFrontSuspension());
        vehicle.setRearSuspension(request.getRearSuspension());
        vehicle.setBrakeSystem(request.getBrakeSystem());
        vehicle.setTrunkCapacity(request.getTrunkCapacity());
        vehicle.setWeightWithoutBattery(request.getWeightWithoutBattery());
        vehicle.setWeightWithBattery(request.getWeightWithBattery());

        return mapToResponse(vehicleRepository.save(vehicle));
    }

    @Override
    public void deleteVehicle(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        vehicleRepository.delete(vehicle);
    }
}
