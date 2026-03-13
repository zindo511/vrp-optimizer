package vn.ttcs.vrp.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "vehicle_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(name = "max_weight_kg", precision = 10, scale = 2, nullable = false)
    private BigDecimal maxWeightKg;

    @Column(name = "max_volume_m3", precision = 10, scale = 2)
    private BigDecimal maxVolumeM3;

    @Column(name = "max_driving_time_minutes")
    private Integer maxDrivingTimeMinutes;

    @Column(name = "cost_per_km", precision = 10, scale = 2, nullable = false)
    private BigDecimal costPerKm;

    @Column(name = "fixed_cost", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal fixedCost = BigDecimal.ZERO;

    @Column(name = "average_speed_kmh", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal averageSpeedKmh = BigDecimal.valueOf(40.0);

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
