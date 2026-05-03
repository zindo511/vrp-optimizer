package vn.ttcs.vrp.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import vn.ttcs.vrp.enums.UserRole;

@Getter
@Setter
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String fullName;
    private UserRole role;
}
