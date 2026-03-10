package vn.ttcs.vrp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "optimization_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OptimizationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", nullable = false)
    private AlgorithmConfig config;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_by", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "run_date", updatable = false)
    private LocalDateTime runDate;

    @Column(name = "total_orders")
    private Integer totalOrders;

    @Column(name = "total_vehicles")
    private Integer totalVehicles;

    @Column(name = "best_fitness", precision = 12, scale = 4)
    private BigDecimal bestFitness;

    @Column(name = "total_distance", precision = 10, scale = 2)
    private BigDecimal totalDistance;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(length = 50)
    private String status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
