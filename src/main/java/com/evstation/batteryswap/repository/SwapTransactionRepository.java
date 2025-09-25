package com.evstation.batteryswap.repository;

import com.evstation.batteryswap.entity.SwapTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SwapTransactionRepository extends JpaRepository<SwapTransaction, Long> {
}
