package vn.ttcs.vrp.model;

import jakarta.persistence.*;
import lombok.*;
import vn.ttcs.vrp.enums.UserRole;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends AuditableEntity{

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.DRIVER;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

}
