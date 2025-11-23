package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.dto.request.LinkVehicleRequest;
import com.evstation.batteryswap.dto.response.*;
import com.evstation.batteryswap.entity.*;
import com.evstation.batteryswap.enums.BatteryStatus;
import com.evstation.batteryswap.enums.SubscriptionStatus;
import com.evstation.batteryswap.repository.*;
import com.evstation.batteryswap.service.InvoiceService;
import com.evstation.batteryswap.service.LinkVehicleService;
import com.evstation.batteryswap.utils.BatterySerialUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LinkVehicleServiceImpl implements LinkVehicleService {

        private final UserRepository userRepository;
        private final VehicleRepository vehicleRepository;
        private final VehicleModelRepository vehicleModelRepository;
        private final SubscriptionPlanRepository subscriptionPlanRepository;
        private final SubscriptionRepository subscriptionRepository;
        private final BatteryRepository batteryRepository;
        private final BatterySerialRepository batterySerialRepository;
        private final SwapTransactionRepository swapTransactionRepository;
        private final InvoiceService invoiceService;

        @Override
        public LinkVehicleResponse linkVehicle(Long userId, LinkVehicleRequest request) {

                // 1️⃣ Lấy user, model và gói đăng ký
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                VehicleModel model = vehicleModelRepository.findById(request.getVehicleModelId())
                                .orElseThrow(() -> new RuntimeException("Vehicle model not found"));

                SubscriptionPlan plan = subscriptionPlanRepository.findById(request.getSubscriptionPlanId())
                                .orElseThrow(() -> new RuntimeException("Subscription plan not found"));

                // 2️⃣ Sinh xe mới cho user
                Vehicle vehicle = new Vehicle();
                vehicle.setVin(generateVin(model.getName()));
                vehicle.setModel(model);
                vehicleRepository.save(vehicle);

                // Gán vehicle cho user
                user.getVehicles().add(vehicle);
                userRepository.save(user);

                // 3️⃣ Kiểm tra subscription ACTIVE trùng
                boolean hasActiveSub = subscriptionRepository
                                .existsByUserIdAndVehicleIdAndStatus(userId, vehicle.getId(),
                                                SubscriptionStatus.ACTIVE);
                if (hasActiveSub) {
                        throw new RuntimeException("User already has an active subscription for this vehicle");
                }

                // 4️⃣ Tạo subscription với status PENDING (chờ thanh toán)
                Subscription subscription = new Subscription();
                subscription.setUser(user);
                subscription.setVehicle(vehicle);
                subscription.setPlan(plan);
                subscription.setStatus(SubscriptionStatus.PENDING); // ⚠️ PENDING cho đến khi thanh toán
                subscription.setStartDate(LocalDate.now());
                subscription.setEndDate(LocalDate.now().plusDays(plan.getDurationDays()));
                subscriptionRepository.save(subscription);

                // 5️⃣ Tạo invoice cho initial subscription payment
                Invoice initialInvoice = invoiceService.createSubscriptionRenewalInvoice(
                                subscription,
                                plan.getPrice(),
                                plan.getName());

                log.info("INITIAL SUBSCRIPTION INVOICE CREATED | userId={} | vehicleId={} | invoiceId={} | amount={}₫ | batterySoH={}%",
                                userId, vehicle.getId(), initialInvoice.getId(), plan.getPrice(),
                                (plan.getMaxSoH() != null ? plan.getMaxSoH() : 100.0));

                // 6️⃣ Lấy model pin mặc định
                Battery batteryModel = batteryRepository.findById(1L)
                                .orElseThrow(() -> new RuntimeException("Battery model not found"));

                // 6️⃣ Sinh pin thật (theo maxBatteries trong gói)
                // SoH được set theo maxSoH của gói (fallback 100% nếu null)
                Double initialSoH = plan.getMaxSoH() != null ? plan.getMaxSoH() : 100.0;

                List<BatterySerial> batterySerials = new ArrayList<>();
                for (int i = 0; i < plan.getMaxBatteries(); i++) {
                        BatterySerial serial = BatterySerial.builder()
                                        .serialNumber(BatterySerialUtil.generateSerialNumber())
                                        .status(BatteryStatus.IN_USE)
                                        .battery(batteryModel)
                                        .vehicle(vehicle)
                                        .station(null) // dealer phát, chưa thuộc trạm
                                        .initialCapacity(batteryModel.getDesignCapacity())
                                        .currentCapacity(batteryModel.getDesignCapacity())
                                        .stateOfHealth(initialSoH) // ⬅️ Dùng maxSoH từ plan
                                        .chargePercent(100.0) // ⬅️ Pin mới luôn đầy 100%
                                        .totalCycleCount(0.0)
                                        .build();

                        batterySerials.add(serial);
                }
                batterySerialRepository.saveAll(batterySerials);

                // 7️⃣ Log phát pin lần đầu
                List<SwapTransaction> logs = new ArrayList<>();
                for (BatterySerial b : batterySerials) {
                        SwapTransaction log = SwapTransaction.builder()
                                        .user(user)
                                        .vehicle(vehicle)
                                        .batterySerial(b)
                                        .station(null)
                                        .timestamp(LocalDateTime.now())
                                        .startPercent(100.0)
                                        .endPercent(100.0)
                                        .depthOfDischarge(0.0)
                                        .degradationThisSwap(0.0)
                                        .energyUsed(0.0)
                                        .cost(0.0)
                                        .build();
                        logs.add(log);
                }
                swapTransactionRepository.saveAll(logs);

                // 8️⃣ KHÔNG tạo swap transaction logs - chờ payment
                // Batteries sẽ được assign sau khi thanh toán

                VehicleSummaryResponse vehicleRes = VehicleSummaryResponse.builder()
                                .id(vehicle.getId())
                                .vin(vehicle.getVin())
                                .model(VehicleModelResponse.builder()
                                                .id(vehicle.getModel().getId())
                                                .name(vehicle.getModel().getName())
                                                .brand(vehicle.getModel().getBrand())
                                                .wheelbase(vehicle.getModel().getWheelbase())
                                                .groundClearance(vehicle.getModel().getGroundClearance())
                                                .seatHeight(vehicle.getModel().getSeatHeight())
                                                .frontTire(vehicle.getModel().getFrontTire())
                                                .rearTire(vehicle.getModel().getRearTire())
                                                .frontSuspension(vehicle.getModel().getFrontSuspension())
                                                .rearSuspension(vehicle.getModel().getRearSuspension())
                                                .weightWithoutBattery(vehicle.getModel().getWeightWithoutBattery())
                                                .trunkCapacity(vehicle.getModel().getTrunkCapacity())
                                                .brakeSystem(vehicle.getModel().getBrakeSystem())
                                                .weightWithBattery(vehicle.getModel().getWeightWithBattery())
                                                .build())
                                .build();

                SubscriptionResponse subRes = SubscriptionResponse.builder()
                                .id(subscription.getId())
                                .planName(plan.getName())
                                .status(subscription.getStatus()) // PENDING
                                .startDate(subscription.getStartDate())
                                .endDate(subscription.getEndDate())
                                .build();

                List<BatterySummaryResponse> batteryRes = batterySerials.stream()
                                .map(b -> BatterySummaryResponse.builder()
                                                .id(b.getId())
                                                .serialNumber(b.getSerialNumber())
                                                .status(b.getStatus().name()) // AVAILABLE
                                                .build())
                                .toList();

                return LinkVehicleResponse.builder()
                                .message("Vehicle created. Please pay invoice #" + initialInvoice.getId()
                                                + " (" + plan.getPrice() + "₫) to activate subscription and receive "
                                                + plan.getMaxBatteries() + " batteries.")
                                .vehicle(vehicleRes)
                                .subscription(subRes)
                                .batteries(batteryRes)
                                .invoiceId(initialInvoice.getId()) // ⚠️ Return invoice ID
                                .invoiceAmount(initialInvoice.getAmount())
                                .build();
        }

        // 🔧 Tạo VIN ngẫu nhiên theo model
        private String generateVin(String modelName) {
                return "VN-" + modelName.toUpperCase().replace(" ", "")
                                + "-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        }
}
