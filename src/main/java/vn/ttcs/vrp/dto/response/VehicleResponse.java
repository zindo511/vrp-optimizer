package vn.ttcs.vrp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import vn.ttcs.vrp.enums.VehicleStatus;

@Getter
@Builder
@AllArgsConstructor
public class VehicleResponse {

    private Long id;
    private VehicleTypeResponse vehicleType;
    private String licensePlate;
    private VehicleStatus status;

}
