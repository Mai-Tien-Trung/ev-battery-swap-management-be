package com.evstation.batteryswap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class BatterySwapStationManagementSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(BatterySwapStationManagementSystemApplication.class, args);
	}

}
