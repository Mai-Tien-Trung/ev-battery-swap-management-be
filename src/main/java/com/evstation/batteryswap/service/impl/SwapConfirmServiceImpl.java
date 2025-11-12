package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.entity.*;
import com.evstation.batteryswap.enums.BatteryStatus;
import com.evstation.batteryswap.enums.SwapTransactionStatus;
import com.evstation.batteryswap.repository.*;
import com.evstation.batteryswap.service.SwapConfirmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SwapConfirmServiceImpl implements SwapConfirmService {

    private final SwapTransactionRepository swapTransactionRepository;
    private final BatterySerialRepository batterySerialRepository;
    private final UserRepository userRepository;

    @Override
    public String confirmSwap(Long transactionId, Long staffId) {

        // 🔍 1️⃣ Lấy giao dịch swap
        SwapTransaction tx = swapTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Swap transaction not found"));

        if (tx.getStatus() != SwapTransactionStatus.PENDING_CONFIRM) {
            throw new RuntimeException("This swap has already been processed");
        }

        // 2️⃣ Lấy pin cũ và trạm liên quan
        BatterySerial oldBattery = tx.getBatterySerial();
        Station station = tx.getStation();

        // 3️⃣ Tìm pin mới đang PENDING_IN tại cùng trạm
        BatterySerial newBattery = batterySerialRepository
                .findFirstByStationIdAndStatus(station.getId(), BatteryStatus.PENDING_IN)
                .orElseThrow(() -> new RuntimeException("No pending battery found for this swap"));

        // 4️⃣ Cập nhật trạng thái pin
        // 🔹 Pin cũ -> trả về trạm, sẵn sàng dùng
        oldBattery.setVehicle(null);
        oldBattery.setStation(station);
        oldBattery.setStatus(BatteryStatus.AVAILABLE);

        // 🔹 Pin mới -> gắn vào xe
        newBattery.setVehicle(tx.getVehicle());
        newBattery.setStation(null);
        newBattery.setStatus(BatteryStatus.IN_USE);

        batterySerialRepository.saveAll(List.of(oldBattery, newBattery));

        // 5️⃣ Cập nhật transaction
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        tx.setStatus(SwapTransactionStatus.COMPLETED);
        tx.setStaff(staff);
        tx.setConfirmedAt(LocalDateTime.now());
        swapTransactionRepository.save(tx);

        log.info("CONFIRM_SWAP | staff={} | txId={} | oldBattery={} -> station={} | newBattery={} -> vehicle={}",
                staff.getUsername(), transactionId,
                oldBattery.getSerialNumber(), station.getId(),
                newBattery.getSerialNumber(), tx.getVehicle().getId());

        return "Swap transaction " + transactionId + " confirmed successfully.";
    }

    @Override
    public String rejectSwap(Long transactionId, Long staffId) {

        // 🔍 1️⃣ Lấy giao dịch swap
        SwapTransaction tx = swapTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Swap transaction not found"));

        if (tx.getStatus() != SwapTransactionStatus.PENDING_CONFIRM) {
            throw new RuntimeException("This swap has already been processed");
        }

        BatterySerial oldBattery = tx.getBatterySerial();

        // 🔁 2️⃣ Hoàn lại pin cũ cho xe
        oldBattery.setStatus(BatteryStatus.IN_USE);
        oldBattery.setVehicle(tx.getVehicle());
        oldBattery.setStation(null);
        batterySerialRepository.save(oldBattery);

        // 🗑️ 3️⃣ Tìm và reset pin mới (PENDING_IN) trong trạm
        Station station = tx.getStation();
        batterySerialRepository.findFirstByStationIdAndStatus(station.getId(), BatteryStatus.PENDING_IN)
                .ifPresent(b -> {
                    b.setStatus(BatteryStatus.AVAILABLE);
                    batterySerialRepository.save(b);
                });

        // 4️⃣ Cập nhật transaction
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        tx.setStatus(SwapTransactionStatus.REJECTED);
        tx.setStaff(staff);
        tx.setConfirmedAt(LocalDateTime.now());
        swapTransactionRepository.save(tx);

        log.warn("REJECT_SWAP | staff={} | txId={} | oldBattery={} | restored to vehicle={}",
                staff.getUsername(), transactionId,
                oldBattery.getSerialNumber(), tx.getVehicle().getId());

        return "Swap transaction " + transactionId + " rejected.";
    }
}
