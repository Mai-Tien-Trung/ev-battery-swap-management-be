package com.evstation.batteryswap.dto.request;

import com.evstation.batteryswap.enums.StationStatus;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StationRequest {

    @NotBlank(message = "Tên trạm không được để trống")
    @Size(max = 100, message = "Tên trạm tối đa 100 ký tự")
    private String name;

    @NotBlank(message = "Địa chỉ không được để trống")
    private String location;

    @NotNull(message = "Trạng thái không được để trống")
    private StationStatus status;

    @Min(value = 1, message = "Sức chứa tối thiểu là 1 pin")
    private int capacity;

    @Pattern(regexp = "^(\\+84|0)\\d{9,10}$", message = "Số điện thoại không hợp lệ")
    private String phone;
}
