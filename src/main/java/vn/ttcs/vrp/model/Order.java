package vn.ttcs.vrp.model;

import jakarta.persistence.*;
import lombok.*;
import vn.ttcs.vrp.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_phone", nullable = false, length = 20)
    private String customerPhone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(name = "total_weight_kg", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalWeightKg;

    @Column(name = "total_volume_m3", precision = 10, scale = 2)
    private BigDecimal totalVolumeM3;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "time_window_from")
    private LocalTime timeWindowFrom;

    @Column(name = "time_window_to")
    private LocalTime timeWindowTo;

    @Column(name = "service_time_minutes")
    @Builder.Default
    private Integer serviceTimeMinutes = 15;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User user;
}
