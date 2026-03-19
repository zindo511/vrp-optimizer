package vn.ttcs.vrp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class VehicleTypeResponse {

    private Long id;
    private String name;
    private BigDecimal maxWeightKg;
    private BigDecimal maxVolumeM3;
    private BigDecimal maxDrivingTimeMinutes;
    private BigDecimal costPerKm;
    private BigDecimal fixedCost;
    private BigDecimal averageSpeedKmh;
    private Boolean isActive;
}
