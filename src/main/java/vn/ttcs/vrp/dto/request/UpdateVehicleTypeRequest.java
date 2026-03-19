package vn.ttcs.vrp.dto.request;

import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class UpdateVehicleTypeRequest {

    private String name;

    @DecimalMin(value = "0.0", inclusive = false, message = "Tải trọng tối đa phải lớn hơn 0")
    private BigDecimal maxWeightKg;

    @DecimalMin(value = "0.0", message = "Thể tích tối đa phải lớn hơn hoặc bằng 0")
    private BigDecimal maxVolumeM3;

    private Integer maxDrivingTimeMinutes;

    @DecimalMin(value = "0.0", message = "Chi phí / km phải lớn hơn hoặc bằng 0")
    private BigDecimal costPerKm;

    @DecimalMin(value = "0.0", message = "Chi phí cố định phải lớn hơn hoặc bằng 0")
    private BigDecimal fixedCost;

    @DecimalMin(value = "0.0", inclusive = false, message = "Tốc độ trung bình phải lớn hơn 0")
    private BigDecimal averageSpeedKmh;

    private Boolean isActive;
}
