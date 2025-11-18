package com.evstation.batteryswap.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Pattern(regexp = "^(\\+84|0)\\d{9,10}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @Size(min = 5, max = 200, message = "Địa chỉ phải từ 5 đến 200 ký tự")
    private String address;
}