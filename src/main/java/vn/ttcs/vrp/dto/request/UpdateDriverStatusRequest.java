package vn.ttcs.vrp.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.ttcs.vrp.enums.DriverStatus;

@Getter
@Setter
@NoArgsConstructor
public class UpdateDriverStatusRequest {

    @NotNull(message = "Trạng thái tài xế không được để trống")
    private DriverStatus driverStatus;
}
