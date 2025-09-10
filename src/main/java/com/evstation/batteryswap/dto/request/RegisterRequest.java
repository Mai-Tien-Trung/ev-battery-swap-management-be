package com.evstation.batteryswap.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data
public class RegisterRequest {

    @NotBlank(message = "Username không được để trống")
    @Size(min = 4, max = 20, message = "Username phải từ 4-20 ký tự")
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Pattern(
            regexp = "^[A-Za-z0-9._%+-]+@gmail\\.com$",
            message = "Email phải có đuôi @gmail.com"
    )
    private String email;

    @NotBlank(message = "Password không được để trống")
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[@#$%^&+=!])(?=\\S+$).{6,}$",
            message = "Password phải có ít nhất 6 ký tự, chứa ít nhất 1 số và 1 ký tự đặc biệt"
    )
    private String password;


}