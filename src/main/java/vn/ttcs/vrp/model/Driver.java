package vn.ttcs.vrp.model;

import jakarta.persistence.*;
import lombok.*;
import vn.ttcs.vrp.enums.DriverStatus;

@Entity
@Table(name = "drivers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "license_number", length = 100, nullable = false, unique = true)
    private String licenseNumber;

    @Column(length = 20, nullable = false)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    @Builder.Default
    private DriverStatus status = DriverStatus.ACTIVE;
}
