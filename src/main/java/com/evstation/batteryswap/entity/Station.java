package com.evstation.batteryswap.entity;

import com.evstation.batteryswap.enums.StationStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Station {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;   // Ví dụ: "Selex Station HN01"

    @Column(nullable = false)
    private String location;  // Địa chỉ hoặc mô tả vị trí

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StationStatus status = StationStatus.ACTIVE;

    private int capacity;

    private String phone;  // Số liên hệ

    @Column
    private Double latitude;   // Vĩ độ

    @Column
    private Double longitude;  // Kinh độ
}
