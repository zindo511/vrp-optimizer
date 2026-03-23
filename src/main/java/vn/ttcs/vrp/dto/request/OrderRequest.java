package vn.ttcs.vrp.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
public class OrderRequest {

    @NotBlank(message = "Tên khách hàng không được để trống")
    @Size(max = 255, message = "Tên khách hàng không được vượt quá 255 ký tự")
    private String customerName;

    @NotBlank(message = "Số điện thoại khách hàng không được để trống")
    @Pattern(regexp = "^[0-9]{10,11}$", message = "Số điện thoại không hợp lệ (10-11 chữ số)")
    private String customerPhone;

    @NotNull(message = "ID địa điểm giao hàng không được để trống")
    private Long locationId;

    @NotNull(message = "Khối lượng hàng hóa không được để trống")
    @DecimalMin(value = "0.01", message = "Khối lượng phải lớn hơn 0")
    @Digits(integer = 8, fraction = 2, message = "Khối lượng không hợp lệ")
    private BigDecimal totalWeightKg;

    @DecimalMin(value = "0.0", inclusive = false, message = "Thể tích phải lớn hơn 0")
    @Digits(integer = 8, fraction = 2, message = "Thể tích không hợp lệ")
    private BigDecimal totalVolumeM3;

    private String note;

    private LocalTime timeWindowFrom;

    private LocalTime timeWindowTo;

    @Min(value = 1, message = "Thời gian phục vụ phải ít nhất 1 phút")
    private Integer serviceTimeMinutes;
}
