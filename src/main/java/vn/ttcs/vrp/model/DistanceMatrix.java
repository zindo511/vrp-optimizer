package vn.ttcs.vrp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "distance_matrix",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "unique_od_pair",
                        columnNames = {"origin_id", "destination_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_distance_matrix_lookup",
                        columnList = "origin_id, destination_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DistanceMatrix {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_id", nullable = false)
    private Location origin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id", nullable = false)
    private Location destination;

    @Column(name = "distance_meters", precision = 10, scale = 2, nullable = false)
    private BigDecimal distanceMeters;

    @Column(name = "duration_seconds", nullable = false)
    private Long durationSeconds;

    @CreationTimestamp
    @Column(name = "calculated_at", updatable = false)
    private LocalDateTime calculatedAt;
}
