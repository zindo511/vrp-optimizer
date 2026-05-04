package vn.ttcs.vrp.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class LocationRequest {

    @NotBlank(message = "Địa chỉ không được bỏ trống")
    private String address;

    @NotNull(message = "Vĩ độ (latitude) không được để trống")
    @DecimalMin(value = "8.0", message = "Vĩ độ phải >= 8.0 (giới hạn phía Nam VN)")
    @DecimalMax(value = "24.0", message = "Vĩ độ phải <= 24.0 (giới hạn phía Bắc VN)")
    private BigDecimal latitude;

    @NotNull(message = "Kinh độ (longitude) không được để trống")
    @DecimalMin(value = "102.0", message = "Kinh độ phải >= 102.0 (giới hạn phía Tây VN)")
    @DecimalMax(value = "110.0", message = "Kinh độ phải <= 110.0 (giới hạn phía Đông VN)")
    private BigDecimal longitude;
}
