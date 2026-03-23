package vn.ttcs.vrp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import vn.ttcs.vrp.enums.DriverStatus;

@Getter
@Builder
@AllArgsConstructor
public class DriverResponse {

    private Long id;
    private Long userId;
    private String userEmail;
    private String userFullName;
    private String licenseNumber;
    private String phone;
    private DriverStatus status;
}
