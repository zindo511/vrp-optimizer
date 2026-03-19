package vn.ttcs.vrp.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class VehicleTypeRequest {

    @NotBlank(message = "Tên loại xe không được để trống")
    private String name;

    @NotNull(message = "Tải trọng tối đa không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Tải trọng tối đa phải lớn hơn 0")
    private BigDecimal maxWeightKg;

    @DecimalMin(value = "0.0", inclusive = false, message = "Thể tích tối đa phải lớn hơn 0")
    private BigDecimal maxVolumeM3;

    @Min(value = 1, message = "Thời gian lái xe tối đa phải lớn hơn 0")
    private Integer maxDrivingTimeMinutes;

    @NotNull(message = "Chi phí mỗi km không được để trống")
    @DecimalMin(value = "0.0", message = "Chi phí không được âm")
    private BigDecimal costPerKm;

    @DecimalMin(value = "0.0", message = "Chi phí cố định không được âm")
    private BigDecimal fixedCost;

    @DecimalMin(value = "0.0", inclusive = false, message = "Tốc độ trung bình phải lớn hơn 0")
    private BigDecimal averageSpeedKmh;

    private Boolean isActive;
}
