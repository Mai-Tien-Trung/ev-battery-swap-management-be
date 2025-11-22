package com.evstation.batteryswap.repository;

import com.evstation.batteryswap.entity.BatterySerial;
import com.evstation.batteryswap.entity.SwapTransaction;
import com.evstation.batteryswap.enums.SwapTransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SwapTransactionRepository extends JpaRepository<SwapTransaction, Long> {
        Optional<SwapTransaction> findTopByBatterySerialOrderByTimestampDesc(BatterySerial batterySerial);

        List<SwapTransaction> findByStatus(SwapTransactionStatus status);

        List<SwapTransaction> findByUserUsernameOrderByTimestampDesc(String username);

        List<SwapTransaction> findByUserIdOrderByTimestampDesc(Long userId);

        @Query(value = "SELECT EXTRACT(HOUR FROM timestamp) AS hour, COUNT(id) AS count " +
                        "FROM swap_transactions " +
                        "GROUP BY hour " +
                        "ORDER BY count DESC, hour ASC", nativeQuery = true)
        List<Object[]> findMostFrequentSwapHour();

        @Query("SELECT s.name as stationName, COUNT(st.id) as swapCount " +
                        "FROM SwapTransaction st " +
                        "JOIN st.station s " +
                        "GROUP BY s.name " +
                        "ORDER BY swapCount DESC")
        List<Object[]> findSwapsPerStation();
}
