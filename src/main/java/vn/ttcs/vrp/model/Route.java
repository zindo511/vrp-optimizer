package vn.ttcs.vrp.model;

import jakarta.persistence.*;
import lombok.*;
import vn.ttcs.vrp.enums.RouteStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "routes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Route extends BaseEntity{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "optimization_result_id")
    private OptimizationResult result;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "start_depot_id", nullable = false)
    private Depot startDepot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "total_distance_meters", precision = 10, scale = 2)
    private BigDecimal totalDistanceMeters;

    @Column(name = "total_duration_seconds")
    private Long totalDurationSeconds;

    @Column(name = "total_weight_kg", precision = 10, scale = 2)
    private BigDecimal totalWeightKg;

    @Column(name = "route_date", nullable = false)
    private LocalDate routeDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    @Builder.Default
    private RouteStatus status = RouteStatus.PLANNED;

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RouteStop> routeStops;
}
