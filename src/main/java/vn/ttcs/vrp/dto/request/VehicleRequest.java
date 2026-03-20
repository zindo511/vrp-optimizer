package vn.ttcs.vrp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VehicleRequest {

    @NotNull(message = "ID của loại xe không được để trống")
    private Long vehicleTypeId;

    @NotBlank(message = "Biển số xe không được để trống")
    private String licensePlate;
}
