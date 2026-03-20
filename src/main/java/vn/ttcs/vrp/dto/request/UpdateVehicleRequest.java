package vn.ttcs.vrp.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.ttcs.vrp.enums.VehicleStatus;

@Getter
@Setter
@NoArgsConstructor
public class UpdateVehicleRequest {

    private Long vehicleTypeId;
    private String licensePlate;
    private VehicleStatus vehicleStatus;
}
