package com.evstation.batteryswap.entity;


import com.evstation.batteryswap.enums.Role;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;   // ✅ đăng nhập bằng username

    @Column(unique = true, nullable = false)
    private String email;      // vẫn giữ email để liên lạc, verify

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
}
