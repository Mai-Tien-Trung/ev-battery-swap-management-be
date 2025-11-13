package com.evstation.batteryswap.repository;

import com.evstation.batteryswap.entity.BatterySerial;
import com.evstation.batteryswap.entity.SwapTransaction;
import com.evstation.batteryswap.enums.SwapTransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SwapTransactionRepository extends JpaRepository<SwapTransaction, Long> {
    Optional<SwapTransaction> findTopByBatterySerialOrderByTimestampDesc(BatterySerial batterySerial);
    List<SwapTransaction> findByStatus(SwapTransactionStatus status);
    List<SwapTransaction> findByUserUsernameOrderByTimestampDesc(String username);

}
