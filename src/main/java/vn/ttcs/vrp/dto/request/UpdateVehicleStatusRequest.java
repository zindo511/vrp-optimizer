package vn.ttcs.vrp.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.ttcs.vrp.enums.VehicleStatus;

@Getter
@Setter
@NoArgsConstructor
public class UpdateVehicleStatusRequest {

    @NotBlank(message = "Status của xe không được để trống")
    private VehicleStatus vehicleStatus;
}
